import java.io.{File, FileOutputStream, SequenceInputStream}
import java.nio.charset.StandardCharsets

import sbt._
import Keys._
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._
import gov.nasa.jpl.imce.sbt._

import scala.xml._
import scala.Double
import scala.language.postfixOps
import scala.math._
import scala.Console

updateOptions := updateOptions.value.withCachedResolution(true)

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in ThisBuild :=
  (baseDirectory in ThisBuild).value / "target" / "md.package"

cleanFiles += (mdInstallDirectory in ThisBuild).value

lazy val root = Project("imce-magicdraw-library-enhanced_api", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(IMCEPlugin.aspectJSettings)
  .settings(
    IMCEKeys.licenseYearOrRange := "2015-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    buildInfoPackage := "imce.magicdraw.library.enhanced_api",
    buildInfoKeys ++= Seq[BuildInfoKey](BuildInfoKey.action("buildDateUTC") { buildUTCDate.value }),

    projectID := {
      val previous = projectID.value
      previous.extra(
        "build.date.utc" -> buildUTCDate.value,
        "artifact.kind" -> "magicdraw.library")
    },

    resourceDirectory in Compile := baseDirectory.value / "resources",

    aspectjSource in Aspectj := baseDirectory.value / "src" / "main" / "aspectj",
    javaSource in Compile := baseDirectory.value / "src" / "main" / "aspectj",

    compileOrder := CompileOrder.ScalaThenJava,

    aspectjVersion in Aspectj := Versions.org_aspectj_version,

    resolvers += Resolver.bintrayRepo("jpl-imce", "gov.nasa.jpl.imce"),
    resolvers += Resolver.bintrayRepo("tiwg", "org.omg.tiwg"),
    resolvers += Resolver.bintrayRepo("tiwg", "org.omg.tiwg.vendor.nomagic"),
    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",

    libraryDependencies ++= Seq(

      "org.omg.tiwg.vendor.nomagic"
        % "com.nomagic.magicdraw.package"
        % "18.0-sp6"
        % "compile"
        artifacts
        Artifact("com.nomagic.magicdraw.package", "pom", "pom", None, Seq(), None, Map()),

      "gov.nasa.jpl.imce" %% "imce.third_party.aspectj_libraries" % Versions_aspectj_libraries.version
        % "compile" artifacts
        Artifact("imce.third_party.aspectj_libraries", "zip", "zip", Some("resource"), Seq(), None, Map())
    ),

    extractArchives := {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value
      val showDownloadProgress = true

      if (!mdInstallDir.exists) {

        downloadMagicDraw(
          s.log, showDownloadProgress,
          up,
          credentials.value,
          mdInstallDir, base / "target" / "no_install.zip")

      } else
        s.log.info(
          s"=> use existing md.install.dir=$mdInstallDir")
    },

    unmanagedJars in Compile ++= {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value
      val _ = extractArchives.value

      val libJars = (mdInstallDir / "lib") ** "*.jar"
      val mdJars = libJars.get.map(Attributed.blank)

      s.log.info(s"=> Adding ${mdJars.size} unmanaged jars")

      mdJars
    },

    compile in Compile := {
      val _ = extractArchives.value
      (compile in Compile).value
    },

    sources in(Compile, doc) := Seq.empty,
    publishArtifact in(Compile, packageDoc) := false
  )

def downloadMagicDraw
(log: Logger,
 showDownloadProgress: Boolean,
 up: UpdateReport,
 credentials: Seq[Credentials],
 mdInstallDir: File,
 mdZip: File): Unit = {

  IO.createDirectory(mdInstallDir)

  val tfilter: DependencyFilter = new DependencyFilter {
    def apply(c: String, m: ModuleID, a: Artifact): Boolean =
      a.extension == "pom" &&
        m.organization.startsWith("org.omg.tiwg.vendor.nomagic") &&
        m.name.startsWith("com.nomagic.magicdraw.package")
  }

  up
    .matching(tfilter)
    .headOption
    .fold[Unit]{
    log.warn("No MagicDraw POM artifact found!")
  }{ pom =>
    // Use unzipURL to download & extract
    //val files = IO.unzip(zip, mdInstallDir)
    val mdNoInstallZipDownloadURL = new URL(((XML.load(pom.absolutePath) \\ "properties") \ "md.core").text)

    log.info(
      s"=> found: ${pom.getName} at $mdNoInstallZipDownloadURL")

    // Get the credentials based on host
    credentials
      .flatMap {
        case dc: DirectCredentials if dc.host == mdNoInstallZipDownloadURL.getHost =>
          Some(dc)
        case _ =>
          None
      }
      .headOption
      .fold[Unit] {
      log.error(
        s"=> failed to get credentials for downloading MagicDraw no_install zip"
      )
    } { mdCredentials =>

      // 1. If no credentials are found, attempt a connection without basic authorization
      // 2. If username and password cannot be extracted (e.g., unsupported FileCredentials),
      //    then throw error
      // 3. If authorization wrong, ensure that SBT aborts

      val connection = mdNoInstallZipDownloadURL.openConnection()

      connection
        .setRequestProperty(
          "Authorization",
          "Basic " + java.util.Base64.getEncoder.encodeToString(
            (mdCredentials.userName + ":" + mdCredentials.passwd)
              .getBytes(StandardCharsets.UTF_8))
        )

      // Download the file into /target
      val size = connection.getContentLengthLong
      val input = connection.getInputStream
      val output = new FileOutputStream(mdZip)

      log.info(s"=> Downloading $size bytes (= ${size / 1024 / 1024} MB)...")

      val bytes = new Array[Byte](1024 * 1024)
      var totalBytes: Double = 0
      Iterator
        .continually(input.read(bytes))
        .takeWhile(-1 != _)
        .foreach { read =>
          totalBytes += read
          output.write(bytes, 0, read)

          if (showDownloadProgress) {
            Console.printf(
              "    %.2f MB / %.2f MB (%.1f%%)\r",
              totalBytes / 1024 / 1024,
              size * 1.0 / 1024.0 / 1024.0,
              (totalBytes / size) * 100)
          }
        }

      output.close()

      // Use unzipURL to download & extract
      val files = IO.unzip(mdZip, mdInstallDir)
      log.info(
        s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
          s"files extracted from zip located at: $mdNoInstallZipDownloadURL")
    }

  }
}

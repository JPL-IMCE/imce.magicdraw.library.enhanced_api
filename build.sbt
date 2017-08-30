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

val extractArchives: TaskKey[Unit] = TaskKey[Unit]("extract-archives", "Extracts ZIP files")

lazy val root = Project("imce-magicdraw-library-enhanced_api", file("."))
  .enablePlugins(IMCEGitPlugin)
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

    scalacOptions in (Compile, compile) += s"-P:artima-supersafe:config-file:${baseDirectory.value}/project/supersafe.cfg",
    scalacOptions in (Test, compile) += s"-P:artima-supersafe:config-file:${baseDirectory.value}/project/supersafe.cfg",
    scalacOptions in (Compile, doc) += "-Xplugin-disable:artima-supersafe",
    scalacOptions in (Test, doc) += "-Xplugin-disable:artima-supersafe",

    // ignore scaladoc warnings about AspectJ tags.
    scalacOptions in (Compile, doc) -= "-Xfatal-warnings",

    libraryDependencies ++= Seq(

      "org.omg.tiwg.vendor.nomagic"
        % "com.nomagic.magicdraw.package"
        % "18.4-sp1.0"
        % "compile"
        artifacts
        Artifact("com.nomagic.magicdraw.package", "pom", "pom"),

      "gov.nasa.jpl.imce" %% "imce.third_party.aspectj_libraries" % Versions_aspectj_libraries.version
        % "compile" artifacts
        Artifact("imce.third_party.aspectj_libraries", "zip", "zip", "resource")
    ),

    extractArchives := {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value
      val showDownloadProgress = true

      if (!mdInstallDir.exists) {

        MagicDrawDownloader.fetchMagicDraw(
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
    }
  )


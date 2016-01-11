import better.files.{File => BFile, _}
import java.io.File
import java.nio.file.Files

import sbt._
import Keys._

import com.typesafe.sbt.pgp.PgpKeys
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._

import scala.xml.{Node => XNode}
import scala.xml.transform._

import scala.collection.JavaConversions._

import gov.nasa.jpl.imce.sbt._

useGpg := true

val cae_artifactory_ext_releases =
  Resolver.url(
    "Artifactory Realm",
    url("https://cae-artrepo.jpl.nasa.gov/artifactory/ext-release-local")
  )(Resolver.mavenStylePatterns)

val cae_artifactory_ext_snapshots =
  Resolver.url(
    "Artifactory Realm",
    url("https://cae-artrepo.jpl.nasa.gov/artifactory/ext-snapshot-local")
  )(Resolver.mavenStylePatterns)

val cae_artifactory_plugin_releases =
  Resolver.url(
    "Artifactory Realm",
    url("https://cae-artrepo.jpl.nasa.gov/artifactory/plugins-release-local")
  )(Resolver.mavenStylePatterns)

val cae_artifactory_plugin_snapshots =
  Resolver.url(
    "Artifactory Realm",
    url("https://cae-artrepo.jpl.nasa.gov/artifactory/plugins-snapshot-local")
  )(Resolver.mavenStylePatterns)

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in ThisBuild :=
  (baseDirectory in ThisBuild).value / "cae.md.package" / ("aspect_scala-" + Versions.version)

cleanFiles <+=
  (baseDirectory in ThisBuild) { base => base / "cae.md.package" }

ivyLoggingLevel := UpdateLogging.Full

logLevel in Compile := Level.Debug

persistLogLevel := Level.Debug

// where to look for resolving library dependencies
fullResolvers ++= Seq(
  new MavenRepository("cae ext-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/ext-release-local"),
  new MavenRepository("cae plugins-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/plugins-release-local")
)

lazy val artifactZipFile = taskKey[File]("Location of the zip artifact file")

lazy val extractArchives = TaskKey[Seq[Attributed[File]]]("extract-archives", "Extracts ZIP files")

lazy val updateInstall = TaskKey[Unit]("update-install", "Update the MD Installation directory")

lazy val md5Install = TaskKey[Unit]("md5-install", "Produce an MD5 report of the MD Installation directory")

lazy val zipInstall = TaskKey[File]("zip-install", "Zip the MD Installation directory")

lazy val enhancedLib = project.in(new File("enhancedLib"))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    homepage := Some(url("https://github.jpl.nasa.gov/mbee-dev/cae.magicdraw.packages.aspectj_scala")),
    organizationHomepage := Some(url("http://cae.jpl.nasa.gov")),

    organization := "gov.nasa.jpl.cae.magicdraw.libraries",

    resourceDirectory in Compile := baseDirectory.value / "resources",

    aspectjSource in Aspectj := baseDirectory.value / "src" / "main" / "aspectj",
    javaSource in Compile := baseDirectory.value / "src" / "main" / "aspectj",

    compileOrder := CompileOrder.ScalaThenJava,

    aspectjVersion in Aspectj := Versions.org_aspectj_version,

    libraryDependencies ++= Seq(
      "gov.nasa.jpl.cae.magicdraw.packages" % "cae_md18_0_sp5_mdk" % Versions.mdk_package % "compile" artifacts
        Artifact("cae_md18_0_sp5_mdk", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "all-scala-libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("all-scala-libraries", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "all-aspectj_libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("all-aspectj_libraries", "zip", "zip")
    ),

    extractArchives <<= (baseDirectory, update, streams,
      mdInstallDirectory in ThisBuild) map {
      (base, up, s, mdInstallDir) =>

        if (!mdInstallDir.exists) {
          val zips: Seq[File] = up.matching(artifactFilter(`type` = "zip", extension = "zip"))
          zips.foreach { zip =>
            val files = IO.unzip(zip, mdInstallDir)
            s.log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip: ${zip.getName}")
          }
        } else
          s.log.info(
            s"=> use existing md.install.dir=$mdInstallDir")

        val libPath = (mdInstallDir / "lib").toPath
        val mdJars = for {
          jar <- Files.walk(libPath).iterator().filter(_.toString.endsWith(".jar")).map(_.toFile)
        } yield Attributed.blank(jar)



        mdJars.toSeq
    },

    unmanagedJars in Compile <++= extractArchives,

    compile <<= (compile in Compile) dependsOn extractArchives,

    // disable scaladoc to avoid the errors:
    // [error] /opt/local/imce/users/nfr/github.imce/cae.magicdraw.package.aspectj_scala/enhancedLib/src/main/scala/gov/nasa/jpl/magicdraw/enhanced/ui/browser/EnhancedBrowserContextAMConfigurator.scala:58: Tag '@Pointcut' is not recognised
    // [error]   /**
    // [error]   ^
    // [error] /opt/local/imce/users/nfr/github.imce/cae.magicdraw.package.aspectj_scala/enhancedLib/src/main/scala/gov/nasa/jpl/magicdraw/enhanced/ui/browser/EnhancedBrowserContextAMConfigurator.scala:90: Tag '@After' is not recognised
    // [error]   /**
    // [error]   ^
    // [error] two errors found
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  )
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(IMCEPlugin.aspectJSettings)

def UpdateProperties(mdInstall: File): RewriteRule = {

  println(s"update properties for md.install=$mdInstall")
  val binDir = mdInstall / "bin"
  require(binDir.exists, binDir)
  val binSub = MD5SubDirectory(
    name = "bin",
    files = IO
      .listFiles(binDir, GlobFilter("*.properties"))
      .sorted.map(MD5.md5File(binDir)))

  val docGenScriptsDir = mdInstall / "DocGenUserScripts"
  require(docGenScriptsDir.exists, docGenScriptsDir)
  val scriptsSub = MD5SubDirectory(
    name = "DocGenUserScripts",
    dirs = IO
      .listFiles(docGenScriptsDir, DirectoryFilter)
      .sorted.map(MD5.md5Directory(docGenScriptsDir)))

  val libDir = mdInstall / "lib"
  require(libDir.exists, libDir)
  val libSub = MD5SubDirectory(
    name = "lib",
    files = IO
      .listFiles(libDir, GlobFilter("*.jar"))
      .sorted.map(MD5.md5File(libDir)))

  val pluginsDir = mdInstall / "plugins"
  require(pluginsDir.exists)
  val pluginsSub = MD5SubDirectory(
    name = "plugins",
    dirs = IO
      .listFiles(pluginsDir, DirectoryFilter)
      .sorted.map(MD5.md5Directory(pluginsDir)))

  val modelsDir = mdInstall / "modelLibraries"
  require(modelsDir.exists, libDir)
  val modelsSub = MD5SubDirectory(
    name = "modelLibraries",
    files = IO
      .listFiles(modelsDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(modelsDir)))

  val profilesDir = mdInstall / "profiles"
  require(profilesDir.exists, libDir)
  val profilesSub = MD5SubDirectory(
    name = "profiles",
    files = IO
      .listFiles(profilesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(profilesDir)))

  val samplesDir = mdInstall / "samples"
  require(samplesDir.exists, libDir)
  val samplesSub = MD5SubDirectory(
    name = "samples",
    files = IO
      .listFiles(samplesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(samplesDir)))

  val all = MD5SubDirectory(
    name= ".",
    sub = Seq(binSub, libSub, pluginsSub, modelsSub, profilesSub, scriptsSub, samplesSub))

  new RewriteRule {

    import spray.json._
    import MD5JsonProtocol._

    override def transform(n: XNode): Seq[XNode] = n match {
      case <md5/> =>
        <md5>
          {all.toJson}
        </md5>
      case _ =>
        n
    }
  }
}

lazy val core = Project("root", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .aggregate(enhancedLib)
  .dependsOn(enhancedLib)
  .settings(artifactZipFile := {
    baseDirectory.value / "target" / "package" / "cae.package.aspectj_scala.zip"
  })
  .settings(addArtifact(Artifact("cae_md18_0_sp5_aspectj_scala", "zip", "zip"), artifactZipFile).settings: _*)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    organization := "gov.nasa.jpl.cae.magicdraw.packages",
    name := "cae_md18_0_sp5_aspectj_scala",
    homepage := Some(url("https://github.jpl.nasa.gov/mbee-dev/cae.magicdraw.packages.aspectj_scala")),
    organizationHomepage := Some(url("http://cae.jpl.nasa.gov")),

    git.baseVersion := Versions.version,

    pomPostProcess <<= (pomPostProcess, mdInstallDirectory in ThisBuild) {
      (previousPostProcess, mdInstallDir) =>
      { (node: XNode) =>
        println(s"original pom:")
        println(node)
        val processedNode: XNode = previousPostProcess(node)
        println(s"processed pom:")
        println(processedNode)
        val mdUpdateDir = UpdateProperties(mdInstallDir)
        val resultNode: XNode = new RuleTransformer(mdUpdateDir)(processedNode)
        println(s"result pom:")
        println(resultNode)
        resultNode
      }
    },

    publish <<= publish dependsOn zipInstall,
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn zipInstall,

    publish <<= publish dependsOn (publish in enhancedLib),
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn (PgpKeys.publishSigned in enhancedLib),

    publishLocal <<= publishLocal dependsOn zipInstall,
    publishLocal <<= publishLocal dependsOn (publishLocal in enhancedLib),

    makePom <<= makePom dependsOn md5Install,

    md5Install <<=
      ((baseDirectory, update, streams,
        mdInstallDirectory in ThisBuild,
        version
        ) map {
        (base, up, s, mdInstallDir, buildVersion) =>

          s.log.info(s"***(2) MD5 of md.install.dir=$mdInstallDir")

      }) dependsOn updateInstall,

    updateInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in ThisBuild,
        artifactZipFile,
        packageBin in Compile in enhancedLib, // enhancedLib's jar file
        packageSrc in Compile in enhancedLib, // enhancedLib's sources file
        packageDoc in Compile in enhancedLib, // enhancedLib's javadoc file
        version
        ) map {
        (base, up, s, mdInstallDir, zip, enhancedLibJar, enhancedLibSrc, enhancedLibDoc, buildVersion) =>

          s.log.info(s"***(1) Updating md.install.dir=$mdInstallDir")

          val fileArtifacts = for {
            cReport <- up.configurations
            if Configurations.Compile.name == cReport.configuration
            oReport <- cReport.details
            mReport <- oReport.modules
            (artifact, file) <- mReport.artifacts
            if "jar" == artifact.extension
          } yield (oReport.organization, oReport.name, file, artifact)

          val fileArtifactsByType = fileArtifacts.groupBy { case (_, _, _, a) =>
            a.`classifier`.getOrElse(a.`type`)
          }
          val jarArtifacts = fileArtifactsByType("jar")
          val srcArtifacts = fileArtifactsByType("sources")
          val docArtifacts = fileArtifactsByType("javadoc")

          val jars = {
            val libs = jarArtifacts.map { case (o, _, jar, _) =>
              s.log.info(s"* copying jar: $o/${jar.name}")
              IO.copyFile(jar, mdInstallDir / "lib" / o / jar.name)
              "lib/" + o + "/" + jar.name
            }
            IO.copyFile(enhancedLibJar, mdInstallDir / "lib" / "jpl" / enhancedLibJar.name)
            libs :+ "lib/jpl/" + enhancedLibJar.name
          }

          val weaverJar: String = {
            val weaverJars = jarArtifacts.flatMap {
              case ("org.aspectj", "aspectjweaver", jar, _) =>
                Some("lib/org.aspectj/" + jar.name)
              case _ =>
                None
            }
            require(1 == weaverJars.size)
            weaverJars.head
          }

          val bootJars = jarArtifacts.flatMap {
            case ("org.scala-lang", "scala-library", jar, _) =>
              Some("lib/org.scala-lang/" + jar.name)
            case ("org.aspectj", "aspectjrt", jar, _) =>
              Some("lib/org.aspectj/" + jar.name)
            case ("org.aspectj", "aspectjweaver", jar, _) =>
              Some("lib/org.aspectj/" + jar.name)
            case _ =>
              None
          }

          val bootClasspathPrefix = bootJars.mkString("", "\\\\:", "\\\\:")

          srcArtifacts.foreach { case (o, _, jar, _) =>
            s.log.info(s"* copying source: $o/${jar.name}")
            IO.copyFile(jar, mdInstallDir / "lib.sources" / o / jar.name)
            "lib.sources/" + o + "/" + jar.name
          }
          IO.copyFile(enhancedLibSrc, mdInstallDir / "lib.sources" / "jpl" / enhancedLibSrc.name)

          docArtifacts.foreach { case (o, _, jar, _) =>
            s.log.info(s"* copying javadoc: $o/${jar.name}")
            IO.copyFile(jar, mdInstallDir / "lib.javadoc" / o / jar.name)
            "lib.javadoc/" + o + "/" + jar.name
          }
          IO.copyFile(enhancedLibDoc, mdInstallDir / "lib.javadoc" / "jpl" / enhancedLibDoc.name)

          val mdBinFolder = mdInstallDir / "bin"
          val mdPropertiesFiles: Seq[File] = mdBinFolder.listFiles(new java.io.FilenameFilter() {
            override def accept(dir: File, name: String): Boolean =
              name.endsWith(".properties")
          })

          mdPropertiesFiles.foreach { mdPropertyFile: File =>

            val mdPropertyName = mdPropertyFile.name
            val unpatchedContents: String = IO.read(mdPropertyFile)

            // Remove "-DLOCALCONFIG\=<value>" or "-DLOCALCONFIG=<value>" regardless of what <value> is.
            val patchedContents1 = unpatchedContents.replaceAll("-DLOCALCONFIG\\\\?=[^ ]* ",
              "")

            // Remove "-DWINCONFIG\=<value>" or "-DWINCONFIG=<value>" regardless of what <value> is.
            val patchedContents2 = patchedContents1.replaceAll("-DWINCONFIG\\\\?=[^ ]* ",
              "")

            // Remove "-Dlocal.config.dir.ext\=<value>" or "-Dlocal.config.dir.ext=<value>" regardless of what <value> is.
            val patchedContents3 = patchedContents2.replaceAll("-Dlocal.config.dir.ext\\\\?=[^ ]* ",
              "")

            // Add AspectJ weaver agent & settings
            val patchedContents4 = patchedContents3.replaceFirst("JAVA_ARGS=(.*)",
              s"JAVA_ARGS=-javaagent:$weaverJar " +
                "-Daj.weaving.verbose\\\\=true " +
                "-Dorg.aspectj.weaver.showWeaveInfo\\\\=true $1")

            // MD config settings
            val patchedContents5 = patchedContents4.replaceFirst("(JAVA_ARGS=.*)",
              "$1 -DLOCALCONFIG\\\\=true " +
                "-DWINCONFIG\\\\=true " +
                "-Dlocal.config.dir.ext\\\\=-aspect_scala-" + Versions.version)

            val patchedContents6 = patchedContents5.replaceFirst("BOOT_CLASSPATH=(.*)",
              "BOOT_CLASSPATH=" + bootClasspathPrefix + "$1")

            val patchedContents7 = patchedContents6.replaceFirst("([^_])CLASSPATH=(.*)",
              jars.mkString("$1CLASSPATH=", "\\\\:", "\\\\:$2"))

            IO.write(file = mdPropertyFile, content = patchedContents7, append = false)
          }
      },

    zipInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in ThisBuild,
        artifactZipFile,
        makePom, scalaBinaryVersion
        ) map {
        (base, up, s, mdInstallDir, zip, pom, sbV) =>

	  import java.nio.file.attribute.PosixFilePermission

          s.log.info(s"\n***(3) Creating the zip: $zip")
          val top: BFile = (base / "cae.md.package").toScala
          val scalaSubDir: Iterator[BFile] = top.glob("*/scala-" + sbV)
          scalaSubDir.foreach { dir: BFile =>
            s.log.info(s"* deleting $dir")
            dir.delete()
          }

          val macosExecutables: Iterator[BFile] = top.glob("*/**/*.app/Content/MacOS/*")
          macosExecutables.foreach { f: BFile =>
	    s.log.info(s"* +X $f")
	    f.addPermission(PosixFilePermission.OWNER_EXECUTE) 
          }
          val windowsExecutables: Iterator[BFile] = top.glob("*/**/*.exe")
          windowsExecutables.foreach { f: BFile =>
	    s.log.info(s"* +X $f")
	    f.addPermission(PosixFilePermission.OWNER_EXECUTE) 
          }
          val javaExecutables: Iterator[BFile] = top.glob("*/jre*/**/bin/*")
          javaExecutables.foreach { f: BFile =>
	    s.log.info(s"* +X $f")
	    f.addPermission(PosixFilePermission.OWNER_EXECUTE) 
          }
          val unixExecutables: Iterator[BFile] = top.glob("*/bin/{magicdraw,submit_issue}")
          unixExecutables.foreach { f: BFile =>
	    s.log.info(s"* +X $f")
	    f.addPermission(PosixFilePermission.OWNER_EXECUTE) 
          }

          val zipDir = zip.getParentFile.toScala
          Cmds.mkdirs(zipDir)

          val zipped: BFile = top.zipTo(zip.toScala)
          s.log.info(s"\n***(3) Created the zip: $zipped")

          zip
      }
  )
  .settings(IMCEPlugin.packageLibraryDependenciesWithoutSourcesSettings)

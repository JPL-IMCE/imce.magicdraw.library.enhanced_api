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
lazy val mdAlternateDirectory = SettingKey[File]("md-alternate-directory", "Alternate MagicDraw Installation Directory")

mdInstallDirectory in ThisBuild :=
  (baseDirectory in ThisBuild).value / "cae.md.package" / ("cae.md18_0sp5.aspectj_scala-" + Versions.version)

mdAlternateDirectory in ThisBuild :=
  (baseDirectory in ThisBuild).value / "cae.md.package" / "no-install"

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

      // This dependency will be used to replace all executables & mac-specific applications from the CAE MDK.

      "com.nomagic.magicdraw.application" % "magicdraw" % Versions.magicdraw_no_install % "compile" artifacts
       Artifact("magicdraw", "zip", "zip"),

      // All executables & mac-specific applications in CAE MDK will be replaced
      // with those from magicdraw's no-install bundle.

      "gov.nasa.jpl.cae.magicdraw.packages" % "cae_md18_0_sp5_mdk" % Versions.mdk_package % "compile" artifacts
        Artifact("cae_md18_0_sp5_mdk", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "all-scala-libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("all-scala-libraries", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "all-aspectj_libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("all-aspectj_libraries", "zip", "zip")
    ),

    resolvers +=  new MavenRepository(
      "cae ext-release-local",
      "https://cae-artrepo.jpl.nasa.gov/artifactory/ext-release-local"),

    extractArchives <<= (baseDirectory, update, streams,
      mdInstallDirectory in ThisBuild,
      mdAlternateDirectory in ThisBuild) map {
      (base, up, s, mdInstallDir, mdAlternateDir) =>

        if (!mdInstallDir.exists) {

          val mdkZip: File =
            singleMatch(up, artifactFilter(name = "cae_md18_0_sp5_mdk", `type` = "zip", extension = "zip"))
          s.log.info(s"=> Extracting CAE MDK: $mdkZip")
          nativeUnzip(mdkZip, mdInstallDir)

          val noInstallZip: File =
            singleMatch(up, artifactFilter(name = "magicdraw", `type` = "zip", extension = "zip"))
          s.log.info(s"=> Extracting MD's no-install: $noInstallZip")
          nativeUnzip(noInstallZip, mdAlternateDir)

          // Find all files in no-install that have the executable flag set
          // and copy their permission flags on their corresponding file
          // in the installation folder.

          val isExecutable = new FileFilter {
            def accept(f: File): Boolean =
              !f.isDirectory && f.canExecute
          }

          val execFiles = (mdAlternateDir ** isExecutable) pair relativeTo(mdAlternateDir)
          s.log.info(s"=> ${execFiles.size} executable files")
          execFiles foreach { case (execFile, execPath) =>
            val installedFile = mdInstallDir / execPath
            if (installedFile.exists) {
              s.log.info(s" - $execPath")
              installedFile.toScala.setPermissions(execFile.toScala.permissions)
            }
          }

          // Find all files in no-install that match the pattern *.exe
          // and copy their permission flags on their corresponding file
          // in the installation folder.

          val isWindowsEXE = new FileFilter {
            def accept(f: File): Boolean =
              !f.isDirectory && (f.name endsWith ".exe")
          }
          val windowsEXEFiles = (mdAlternateDir ** isWindowsEXE) pair relativeTo(mdAlternateDir)
          s.log.info(s"=> ${windowsEXEFiles.size} windows *.exe files")
          windowsEXEFiles foreach { case (exeFile, exePath) =>
            val installedFile = mdInstallDir / exePath
            if (installedFile.exists) {
              import java.nio.file.attribute.PosixFilePermission
              s.log.info(s" - $exePath")
              installedFile.toScala.addPermission(PosixFilePermission.OWNER_EXECUTE)
              installedFile.toScala.addPermission(PosixFilePermission.GROUP_EXECUTE)
              installedFile.toScala.addPermission(PosixFilePermission.OTHERS_EXECUTE)
            }
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
    sources in(Compile, doc) := Seq.empty,
    publishArtifact in(Compile, packageDoc) := false
  )
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(IMCEPlugin.aspectJSettings)

def nativeCopyFile(from: File, to: File): Unit = {
  val target = to.getParentFile
  if (target.exists() && target.canWrite) {
    Process(Seq("cp", "-p", from.getAbsolutePath, to.getAbsolutePath), target).! match {
      case 0 => ()
      case n => sys.error("Failed to run native cp application!")
    }
  }
}

def nativeCopyDirectory(from: File, to: File): Unit = {
  Process(Seq("cp", "-rp", from.getAbsolutePath, to.getAbsolutePath), to.getParentFile).! match {
    case 0 => ()
    case n => sys.error("Failed to run native cp application!")
  }
}

def nativeUnzip(f: File, dir: File): Unit = {
  IO.createDirectory(dir)
  Process(Seq("unzip", "-q", f.getAbsolutePath, "-d", dir.getAbsolutePath), dir).! match {
    case 0 => ()
    case n => sys.error("Failed to run native unzip application!")
  }
}

def singleMatch(up: UpdateReport, f: DependencyFilter): File = {
  val files: Seq[File] = up.matching(f)
  require(1 == files.size)
  files.head
}

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
    name = ".",
    sub = Seq(binSub, libSub, pluginsSub, modelsSub, profilesSub, scriptsSub, samplesSub))

  new RewriteRule {

    import spray.json._
    import MD5JsonProtocol._

    override def transform(n: XNode): Seq[XNode] = n match {
      case <md5></md5> =>
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

    projectID := {
      import java.util.{ Date, TimeZone }
      val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
      val timestamp = formatter.format(new Date)

      val previous = projectID.value
      previous.extra("buildDate.UTC" -> timestamp)
    },

    pomPostProcess <<= (pomPostProcess, mdInstallDirectory in ThisBuild) {
      (previousPostProcess, mdInstallDir) => { (node: XNode) =>
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

    // disable publishing the main jar produced by `package`
    publishArtifact in(Compile, packageBin) := false,

    // disable publishing the main API jar
    publishArtifact in(Compile, packageDoc) := false,

    // disable publishing the main sources jar
    publishArtifact in(Compile, packageSrc) := false,

    // disable publishing the jar produced by `test:package`
    publishArtifact in(Test, packageBin) := false,

    // disable publishing the test API jar
    publishArtifact in(Test, packageDoc) := false,

    // disable publishing the test sources jar
    publishArtifact in(Test, packageSrc) := false,

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

            // Remove "-Dlocal.config.dir.ext\=<value>" or "-Dlocal.config.dir.ext=<value>" regardless of what <value> is.
            val patchedContents1 = unpatchedContents.replaceAll(
              "-Dlocal.config.dir.ext\\\\?=[a-zA-Z0-9_.\\\\-]*",
              "-Dlocal.config.dir.ext\\\\=-aspectj_scala-" + Versions.version)

            // Add AspectJ weaver agent & settings
            val patchedContents2 = patchedContents1.replaceFirst(
              "JAVA_ARGS=",
              s"JAVA_ARGS=-javaagent:$weaverJar " +
                "-Daj.weaving.verbose\\\\=true " +
                "-Dorg.aspectj.weaver.showWeaveInfo\\\\=true ")

            val patchedContents3 = patchedContents2.replaceFirst(
              "BOOT_CLASSPATH=",
              "BOOT_CLASSPATH=" + bootClasspathPrefix)

            val patchedContents4 = patchedContents3.replaceFirst(
              "([^_])CLASSPATH=(.*)",
              jars.mkString("$1CLASSPATH=", "\\\\:", "\\\\:$2"))

            IO.write(file = mdPropertyFile, content = patchedContents4, append = false)
          }
      },

    zipInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in ThisBuild,
        artifactZipFile,
        makePom, scalaBinaryVersion
        ) map {
        (base, up, s, mdInstallDir, zip, pom, sbV) =>

          s.log.info(s"\n# Creating the zip: $zip")
          IO.zip(allSubpaths(mdInstallDir), zip)

          zip
      }
  )
  .settings(IMCEReleasePlugin.packageReleaseProcessSettings)
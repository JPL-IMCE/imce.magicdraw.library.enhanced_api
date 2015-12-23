import java.io.File
import java.nio.file.Files

import com.typesafe.sbt.SbtGit
import com.typesafe.sbt.git.{ReadableGit, DefaultReadableGit}
import sbt._
import Keys._

import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._

import org.apache.ivy.core.module.descriptor.{DependencyDescriptor, ModuleDescriptor}
import org.apache.ivy.util.extendable.ExtendableItem

import scala.xml.{Node => XNode}
import scala.xml.transform._

import scala.collection.JavaConversions._

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

mdInstallDirectory in ThisBuild := (baseDirectory in ThisBuild).value / "cae.md.package" /  Versions.aspectj_scala_package_B

cleanFiles <+=
  (baseDirectory in ThisBuild) { base => base / "cae.md.package" }

git.useGitDescribe in ThisBuild := true

ivyLoggingLevel := UpdateLogging.Full

logLevel in Compile := Level.Debug

persistLogLevel := Level.Debug

val noSourcesSettings: Seq[Setting[_]] = Seq(

  // disable using the project's base directory as a source directory
  sourcesInBase := false,

  // Map artifact ModuleID to a Maven-style path for publishing/lookup on the repo
  publishMavenStyle := true,

  // where to publish artifacts
  publishTo := Some(cae_artifactory_ext_releases),

  // where to look for resolving library dependencies
  fullResolvers ++= Seq(new MavenRepository("cae ext-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/ext-release-local"),
                        new MavenRepository("cae plugins-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/plugins-release-local")
                    ),

  scalaVersion := "2.11.7",

  // disable automatic dependency on the Scala library
  autoScalaLibrary := false,

  // disable automatic dependency on the Scala tools used to run SBT itself
  //managedScalaInstance := false,

  // disable using the Scala version in output paths and artifacts
  crossPaths := false,

  // disable publishing the main jar produced by `package`
  publishArtifact in (Compile, packageBin) := false,

  // disable publishing the main API jar
  publishArtifact in (Compile, packageDoc) := false,

  // disable publishing the main sources jar
  publishArtifact in (Compile, packageSrc) := false,

  pomAllRepositories := true,

  makePomConfiguration :=
    makePomConfiguration.value.copy(includeTypes = Set(Artifact.DefaultType, Artifact.PomType, "zip"))

)

def moduleSettings(moduleID: ModuleID): Seq[Setting[_]] =
  Seq(
    name := moduleID.name,
    organization := moduleID.organization,
    version := moduleID.revision
  )

lazy val artifactZipFile = taskKey[File]("Location of the zip artifact file")

lazy val extractArchives = TaskKey[Seq[Attributed[File]]]("extract-archives", "Extracts ZIP files")

lazy val updateInstall = TaskKey[Unit]("update-install", "Update the MD Installation directory")

lazy val md5Install = TaskKey[Unit]("md5-install", "Produce an MD5 report of the MD Installation directory")

lazy val zipInstall = TaskKey[File]("zip-install", "Zip the MD Installation directory")

// from sbt-git
lazy val gitReader = SettingKey[ReadableGit]("git-reader", "This gives us a read-only view of the git repository.")
lazy val gitCurrentBranch = SettingKey[String]("git-current-branch", "The current branch for this project.")
lazy val gitCurrentTags = SettingKey[Seq[String]]("git-current-tags", "The tags associated with this commit.")
lazy val gitHeadCommit = SettingKey[Option[String]]("git-head-commit", "The commit sha for the top commit of this project.")
lazy val gitDescribedVersion = SettingKey[Option[String]]("git-described-version", "Version as returned by `git describe --tags`.")
lazy val gitUncommittedChanges = SettingKey[Boolean]("git-uncommitted-changes", "Whether there are uncommitted changes.")

// step1
val mdk_package_ID = "gov.nasa.jpl.cae.magicdraw.packages" % Versions.mdk_package_N % Versions.mdk_package_V
val mdk_package_A = Artifact(mdk_package_ID.name, "zip", "zip")

lazy val enhancedLib = Project("enhancedLib", file("enhancedLib"))
    .enablePlugins(GitBranchPrompt)
    .settings(aspectjSettings : _*)
    .settings(
      gitReader := new DefaultReadableGit(baseDirectory.value.getParentFile),
      gitCurrentBranch := Option(gitReader.value.withGit(_.branch)).getOrElse(""),
      gitCurrentTags := gitReader.value.withGit(_.currentTags),
      gitHeadCommit := gitReader.value.withGit(_.headCommitSha),
      gitDescribedVersion := gitReader.value.withGit(_.describedVersion).map(v => git.gitTagToVersionNumber.value(v).getOrElse(v)),
      gitUncommittedChanges := gitReader.value.withGit(_.hasUncommittedChanges),

      homepage := Some(url("https://github.jpl.nasa.gov/mbee-dev/" + Versions.aspectj_scala_package_P)),
      organizationHomepage := Some(url("http://cae.jpl.nasa.gov")),

      version := Versions.aspectj_scala_package_B,

      // Map artifact ModuleID to a Maven-style path for publishing/lookup on the repo
      publishMavenStyle := true,

      // where to publish artifacts
      publishTo := Some(cae_artifactory_ext_releases),

      // where to look for resolving library dependencies
      fullResolvers ++= Seq(new MavenRepository("cae ext-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/ext-release-local"),
        new MavenRepository("cae plugins-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/plugins-release-local")
      ),

      // include all repositories in the POM
      pomAllRepositories := true,

      // include *.zip artifacts in the POM dependency section
      makePomConfiguration :=
        makePomConfiguration.value.copy(includeTypes = Set(Artifact.DefaultType, Artifact.PomType, "zip")),

      pomExtra :=
        <properties>
          <git.branch>{gitCurrentBranch.value}</git.branch>
          <git.commit>{gitHeadCommit.value.getOrElse("N/A")+(if (gitUncommittedChanges.value) "-SNAPSHOT" else "")}</git.commit>
          <git.tags>{gitCurrentTags.value}</git.tags>
        </properties>,

      organization := "gov.nasa.jpl.cae.magicdraw.libraries",

      scalaVersion := "2.11.7",

      // disable automatic dependency on the Scala library
      autoScalaLibrary := false,

      scalacOptions += "-g:vars",
      javacOptions += "-g:vars",

      extraAspectjOptions in Aspectj := Seq("-g"),

      // only compile the aspects (no weaving)
      compileOnly in Aspectj := true,

      // add the compiled aspects as products
      products in Compile <++= products in Aspectj,

      resourceDirectory in Compile := baseDirectory.value / "resources",

      aspectjSource in Aspectj := baseDirectory.value / "src" / "main" / "aspectj",
      javaSource in Compile := baseDirectory.value / "src" / "main" / "aspectj",

      compileOrder := CompileOrder.ScalaThenJava,

      aspectjVersion in Aspectj := Versions.org_aspectj_version,

      libraryDependencies ++= Seq(
        mdk_package_ID % "compile" artifacts mdk_package_A,

        // Scala
        "org.scala-lang" % "scala-library" %
          Versions.scala_version % "compile" withSources() withJavadoc(),

        "org.scala-lang" % "scala-compiler" %
          Versions.scala_version % "compile" withSources() withJavadoc(),

        "org.scala-lang" % "scala-reflect" %
          Versions.scala_version % "compile" withSources() withJavadoc(),

        "org.scala-lang" % "scalap" %
          Versions.scala_version % "compile" withSources() withJavadoc(),

        "org.scala-lang.modules" % s"scala-xml_${Versions.scala_binary}" %
          Versions.scala_xml_version % "compile" withSources() withJavadoc(),

        "org.scala-lang.modules" % s"scala-parser-combinators_${Versions.scala_binary}" %
          Versions.scala_parser_combinators_version % "compile" withSources() withJavadoc(),

        "org.scala-lang.modules" % s"scala-swing_${Versions.scala_binary}" %
          Versions.scala_swing_version % "compile" withSources() withJavadoc(),

        "org.scala-lang.plugins" % s"scala-continuations-library_${Versions.scala_binary}" %
          Versions.scala_continuations_version % "compile" withSources() withJavadoc(),

        // AspectJ
        "org.aspectj" % "aspectjrt" %
          Versions.org_aspectj_version % "compile" withSources() withJavadoc(),

        "org.aspectj" % "aspectjtools" %
          Versions.org_aspectj_version % "compile" withSources() withJavadoc(),

        "org.aspectj" % "aspectjweaver" %
          Versions.org_aspectj_version % "compile" withSources() withJavadoc()
      ),

      extractArchives <<= (baseDirectory, update, streams,
                           mdInstallDirectory in ThisBuild) map {
        (base, up, s, mdInstallDir) =>

          if (!mdInstallDir.exists) {
            val zips: Seq[File] = up.matching(artifactFilter(`type` = "zip", extension = "zip"))
            zips.foreach { zip =>
              val files = IO.unzip(zip, mdInstallDir)
              s.log.info(
                s"=> created md.install.dir=$mdInstallDir with ${files.size} "+
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

      compile <<= (compile in Compile) dependsOn extractArchives
    )
    .settings(aspectjDependencySettings : _*)

def UpdateProperties(mdInstall: File): RewriteRule = {

  println(s"update properties for md.install=$mdInstall")
  val binDir = mdInstall / "bin"
  require(binDir.exists, binDir)
  val binSub = MD5SubDirectory(
    name="bin",
    files=IO.listFiles(binDir, GlobFilter("*.properties")).sorted.map(MD5.md5File(binDir)))

  val docGenScriptsDir = mdInstall / "DocGenUserScripts"
  require(docGenScriptsDir.exists, docGenScriptsDir)
  val scriptsSub = MD5SubDirectory(
    name="DocGenUserScripts",
    dirs=IO.listFiles(docGenScriptsDir,DirectoryFilter).sorted.map(MD5.md5Directory(docGenScriptsDir)))

  val libDir = mdInstall / "lib"
  require(libDir.exists, libDir)
  val libSub = MD5SubDirectory(
    name="lib",
    files=IO.listFiles(libDir, GlobFilter("*.jar")).sorted.map(MD5.md5File(libDir)))

  val pluginsDir = mdInstall / "plugins"
  require(pluginsDir.exists)
  val pluginsSub = MD5SubDirectory(
    name="plugins",
    dirs=IO.listFiles(pluginsDir,DirectoryFilter).sorted.map(MD5.md5Directory(pluginsDir)))

  val modelsDir = mdInstall / "modelLibraries"
  require(modelsDir.exists, libDir)
  val modelsSub = MD5SubDirectory(
    name="modelLibraries",
    files=IO.listFiles(modelsDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml")).sorted.map(MD5.md5File(modelsDir)))

  val profilesDir = mdInstall / "profiles"
  require(profilesDir.exists, libDir)
  val profilesSub = MD5SubDirectory(
    name="profiles",
    files=IO.listFiles(profilesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml")).sorted.map(MD5.md5File(profilesDir)))

  val samplesDir = mdInstall / "samples"
  require(samplesDir.exists, libDir)
  val samplesSub = MD5SubDirectory(
    name="samples",
    files=IO.listFiles(samplesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml")).sorted.map(MD5.md5File(samplesDir)))

  val all = MD5SubDirectory(".", sub=Seq(binSub, libSub, pluginsSub, modelsSub, profilesSub, samplesSub))

  new RewriteRule {
    import spray.json._
    import MD5JsonProtocol._

    override def transform(n: XNode): Seq[XNode] = n match {
      case <md5/> =>
        <md5>{all.toJson}</md5>
      case _ =>
        n
    }
  }
}

lazy val core = Project("root", file("."))
  .enablePlugins(GitBranchPrompt)
  .settings(noSourcesSettings)
  .aggregate(enhancedLib)
  .dependsOn(enhancedLib)
  .settings(artifactZipFile := { baseDirectory.value / "target" / "package" / Versions.aspectj_scala_package_Z })
  .settings(addArtifact( Artifact(Versions.aspectj_scala_package_N, "zip", "zip"), artifactZipFile ).settings: _*)
  .settings(
    gitReader := new DefaultReadableGit(baseDirectory.value),
    gitCurrentBranch := Option(gitReader.value.withGit(_.branch)).getOrElse(""),
    gitCurrentTags := gitReader.value.withGit(_.currentTags),
    gitHeadCommit := gitReader.value.withGit(_.headCommitSha),
    gitDescribedVersion := gitReader.value.withGit(_.describedVersion).map(v => git.gitTagToVersionNumber.value(v).getOrElse(v)),
    gitUncommittedChanges := gitReader.value.withGit(_.hasUncommittedChanges),

    organization := "gov.nasa.jpl.cae.magicdraw.packages",
    name := Versions.aspectj_scala_package_N,
    homepage := Some(url("https://github.jpl.nasa.gov/mbee-dev/" + Versions.aspectj_scala_package_P)),
    organizationHomepage := Some(url("http://cae.jpl.nasa.gov")),

    version := Versions.aspectj_scala_package_B,

    pomExtra :=
      <properties>
        <git.branch>{gitCurrentBranch.value}</git.branch>
        <git.commit>{gitHeadCommit.value.getOrElse("N/A")+(if (gitUncommittedChanges.value) "-SNAPSHOT" else "")}</git.commit>
        <git.tags>{gitCurrentTags.value}</git.tags>
        <md5/>
      </properties>,

    pomPostProcess := { (node: XNode) =>
      new RuleTransformer(UpdateProperties((mdInstallDirectory in ThisBuild).value))(node)
    },

    publish <<= publish dependsOn zipInstall,
    publish <<= publish dependsOn (publish in enhancedLib),

    publishLocal <<= publishLocal dependsOn zipInstall,
    publishLocal <<= publishLocal dependsOn (publishLocal in enhancedLib),

    makePom <<= makePom dependsOn md5Install,

    md5Install <<=
      ( baseDirectory, update, streams,
        mdInstallDirectory in ThisBuild,
        version
        ) map {
        (base, up, s, mdInstallDir, buildVersion) =>

          s.log.info(s"***(2) MD5 of md.install.dir=$mdInstallDir")

      },

    updateInstall <<=
      ( baseDirectory, update, streams,
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

        val fileArtifactsByType = fileArtifacts.groupBy { case (_, _, _, artifact) =>
          artifact.`classifier`.getOrElse(artifact.`type`)
        }
        val jarArtifacts = fileArtifactsByType("jar")
        val srcArtifacts = fileArtifactsByType("sources")
        val docArtifacts = fileArtifactsByType("javadoc")

        val jars = {
          val libs = jarArtifacts.map { case (organization, _, jar, _) =>
            s.log.info(s"jar: $organization/${jar.name}")
            IO.copyFile(jar, mdInstallDir / "lib" / organization / jar.name)
            "lib/" + organization + "/" + jar.name
          }
          IO.copyFile(enhancedLibJar, mdInstallDir / "lib" / "jpl" / enhancedLibJar.name)
          libs :+ "lib/jpl/" + enhancedLibJar.name
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

        val bootClasspathPrefix = bootJars.mkString("","\\\\:","\\\\:")

        srcArtifacts.foreach { case (organization, _, jar, _) =>
          s.log.info(s"source: $organization/${jar.name}")
          IO.copyFile(jar, mdInstallDir / "lib.sources" / organization / jar.name)
          "lib.sources/" + organization + "/" + jar.name
        }
        IO.copyFile(enhancedLibSrc, mdInstallDir / "lib.sources" / "jpl" / enhancedLibSrc.name)

        docArtifacts.foreach { case (organization, _, jar, _) =>
          s.log.info(s"javadoc: $organization/${jar.name}")
          IO.copyFile(jar, mdInstallDir / "lib.javadoc" / organization / jar.name)
          "lib.javadoc/" + organization + "/" + jar.name
        }
        IO.copyFile(enhancedLibDoc, mdInstallDir / "lib.javadoc" / "jpl" / enhancedLibDoc.name)

        val mdBinFolder = mdInstallDir / "bin"
        val mdPropertiesFiles: Seq[File] = mdBinFolder.listFiles(new java.io.FilenameFilter(){
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
            "JAVA_ARGS=-javaagent:lib/org.aspectj/aspectjweaver.jar "+
            "-Daj.weaving.verbose\\\\=true "+
            "-Dorg.aspectj.weaver.showWeaveInfo\\\\=true $1")

          // MD config settings
          val patchedContents5 = patchedContents4.replaceFirst("(JAVA_ARGS=.*)",
            "$1 -DLOCALCONFIG\\\\=true "+
            "-DWINCONFIG\\\\=true "+
            "-Dlocal.config.dir.ext\\\\=-" + Versions.aspectj_scala_package_B + buildVersion)

          val patchedContents6 = patchedContents5.replaceFirst("BOOT_CLASSPATH=(.*)",
            "BOOT_CLASSPATH="+bootClasspathPrefix+"$1")

          val patchedContents7 = patchedContents6.replaceFirst("([^_])CLASSPATH=(.*)",
            jars.mkString("$1CLASSPATH=","\\\\:","\\\\:$2"))

          IO.write(file=mdPropertyFile, content=patchedContents7, append=false)
        }
    },

    zipInstall <<=
      ( baseDirectory, update, streams,
        mdInstallDirectory in ThisBuild,
        artifactZipFile,
        makePom
        ) map {
        (base, up, s, mdInstallDir, zip, pom) =>

          s.log.info(s"\n***(3) Creating the zip: $zip")
          IO.zip(allSubpaths(base / "cae.md.package"), zip)

          zip
      }
  )

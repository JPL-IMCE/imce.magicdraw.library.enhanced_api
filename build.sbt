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

logLevel in Compile := Level.Debug

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

// where to look for resolving library dependencies
fullResolvers ++= Seq(
  new MavenRepository("cae ext-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/ext-release-local"),
  new MavenRepository("cae plugins-release-local", "https://cae-artrepo.jpl.nasa.gov/artifactory/plugins-release-local")
)

lazy val root = Project("imce-magicdraw-library-enhanced_api", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    homepage := Some(url("https://github.jpl.nasa.gov/imce/imce.magicdraw.library.enhanced_api")),
    organizationHomepage := Some(url("http://imce.jpl.nasa.gov")),

    organization := "gov.nasa.jpl.imce.magicdraw.libraries",

    resourceDirectory in Compile := baseDirectory.value / "resources",

    aspectjSource in Aspectj := baseDirectory.value / "src" / "main" / "aspectj",
    javaSource in Compile := baseDirectory.value / "src" / "main" / "aspectj",

    compileOrder := CompileOrder.ScalaThenJava,

    aspectjVersion in Aspectj := Versions.org_aspectj_version,

    libraryDependencies ++= Seq(

      "gov.nasa.jpl.cae.magicdraw.packages" % "cae_md18_0_sp5_vendor" % Versions.vendor_package % "compile" artifacts
        Artifact("cae_md18_0_sp5_vendor", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "aspectj_libraries" % Versions_aspectj_libraries.version % "compile" artifacts
        Artifact("aspectj_libraries", "zip", "zip", Some("resource"), Seq(), None, Map())
    ),

    resolvers +=  new MavenRepository(
      "cae ext-release-local",
      "https://cae-artrepo.jpl.nasa.gov/artifactory/ext-release-local"),

    extractArchives <<= (baseDirectory, update, streams,
      mdInstallDirectory in ThisBuild,
      mdAlternateDirectory in ThisBuild) map {
      (base, up, s, mdInstallDir, mdAlternateDir) =>

        if (!mdInstallDir.exists) {

          val vendorZip: File =
            singleMatch(up, artifactFilter(name = "cae_md18_0_sp5_vendor", `type` = "zip", extension = "zip"))
          s.log.info(s"=> Extracting CAE Vendor: $vendorZip")
          nativeUnzip(vendorZip, mdInstallDir)

        } else
          s.log.info(
            s"=> use existing md.install.dir=$mdInstallDir")
    },

    unmanagedJars in Compile <++= (baseDirectory, update, streams,
      mdInstallDirectory in ThisBuild,
      extractArchives) map {
      (base, up, s, mdInstallDir, _) =>

        val libJars = (mdInstallDir / "lib") ** "*.jar"
        val mdJars = libJars.get.map(Attributed.blank(_))

        s.log.info(s"=> Adding ${mdJars.size} unmanaged jars")

        mdJars
    },

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

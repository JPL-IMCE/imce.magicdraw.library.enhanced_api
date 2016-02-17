import java.io.File

import sbt._
import Keys._

import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._

import gov.nasa.jpl.imce.sbt._

useGpg := true

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in ThisBuild :=
  (baseDirectory in ThisBuild).value / "cae.md.package"

cleanFiles += (mdInstallDirectory in ThisBuild).value

lazy val root = Project("imce-magicdraw-library-enhanced_api", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(IMCEPlugin.aspectJSettings)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    buildInfoPackage := "imce.magicdraw.library.enhanced_api",
    buildInfoKeys ++= Seq[BuildInfoKey](BuildInfoKey.action("buildDateUTC") { buildUTCDate.value }),

    homepage := Some(url("https://github.jpl.nasa.gov/imce/imce.magicdraw.library.enhanced_api")),
    organizationHomepage := Some(url("http://imce.jpl.nasa.gov")),

    organization := "gov.nasa.jpl.imce.magicdraw.libraries",

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

    libraryDependencies ++= Seq(

      // extra("artifact.kind" -> "magicdraw.package.zip")
      "gov.nasa.jpl.cae.magicdraw.packages" % "cae_md18_0_sp5_vendor" % Versions_cae_vendor_package.version
        artifacts Artifact("cae_md18_0_sp5_vendor", "zip", "zip", Some("resource"), Seq(), None, Map()),

      "gov.nasa.jpl.imce.thirdParty" %% "aspectj_libraries" % Versions_aspectj_libraries.version
        % "compile" artifacts
        Artifact("aspectj_libraries", "zip", "zip", Some("resource"), Seq(), None, Map())
    ),

    extractArchives <<= (baseDirectory, update, streams, mdInstallDirectory in ThisBuild) map {
      (base, up, s, mdInstallDir) =>

        if (!mdInstallDir.exists) {

          val pairs = for {
            cReport <- up.configurations
            mReport <- cReport.modules
            //if mReport.module.extraAttributes.get("artifact.kind").toIterator.contains("magicdraw.package.zip")
            if mReport.module.organization == "gov.nasa.jpl.cae.magicdraw.packages"
            (artifact, archive) <- mReport.artifacts
          } yield artifact -> archive

          val mdZips = pairs.toMap
          require(mdZips.size == 1)
          mdZips.foreach { case (a, f) =>
              s.log.info(s" => Extracting $a")
              IO.unzip(f, mdInstallDir)
          }

        } else
          s.log.info(
            s"=> use existing md.install.dir=$mdInstallDir")
    },

    unmanagedJars in Compile <++= (baseDirectory, update, streams,
      mdInstallDirectory in ThisBuild, extractArchives) map {
      (base, up, s, mdInstallDir, _) =>

        val libJars = (mdInstallDir / "lib") ** "*.jar"
        val mdJars = libJars.get.map(Attributed.blank)

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

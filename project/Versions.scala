object Versions {

  val scala_binary = "2.11"
  val scala_version = "2.11.7"
  // Scala library modules
  val scala_xml_version = "1.0.4"
  val scala_parser_combinators_version = "1.0.4"
  val scala_swing_version = "1.0.2"
  // Scala compiler plugins
  val scala_continuations_version = "1.0.2"

  val cae_md_package_P_prefix = "cae.magicdraw.packages"
  val cae_md_package_N_prefix = "cae_md18_0_sp5"
  val cae_artifact_file_prefix = "cae.package"

  val mdk_package_N = cae_md_package_N_prefix + "_mdk"
  val mdk_package_V = "2.3"

  val org_aspectj_version = "1.8.7"

  val aspectj_scala_package_P = cae_md_package_P_prefix + ".aspectj_scala"
  val aspectj_scala_package_Z = cae_artifact_file_prefix + ".aspectj_scala.zip"
  val aspectj_scala_package_N = cae_md_package_N_prefix + "_aspectj_scala"
  val aspectj_scala_package_B = "aspectj_scala-build-"+sys.props.getOrElse("BUILD_NUMBER", "1")
}
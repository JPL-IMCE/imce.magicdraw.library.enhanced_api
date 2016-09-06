
sbtPlugin := false

name := "imce.magicdraw.library.enhanced_api"

description := "AspectJ enhancements of the MagicDraw 18 Browser & Diagram selection UI APIs"

moduleName := name.value

organization := "gov.nasa.jpl.imce"

homepage := Some(url(s"https://github.com/JPL-IMCE/${moduleName.value}"))

organizationName := "JPL-IMCE"

organizationHomepage := Some(url(s"https://github.com/JPL-IMCE"))

git.remoteRepo := s"git@github.com:JPL-IMCE/${moduleName.value}"

startYear := Some(2015)

scmInfo := Some(ScmInfo(
  browseUrl = url(s"https://github.com/JPL-IMCE/${moduleName.value}"),
  connection = "scm:"+git.remoteRepo.value))

developers := List(
  Developer(
    id="NicolasRouquette",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://github.com/NicolasRouquette")),
  Developer(
    id="TylerRyan",
    name="Tyler J. Ryan",
    email="tyler.j.ryan@jpl.nasa.gov",
    url=url("https://gateway.jpl.nasa.gov/personal/tjryan/default.aspx")))


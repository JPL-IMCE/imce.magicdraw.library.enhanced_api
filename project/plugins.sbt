logLevel := Level.Warn

// https://github.com/sbt/sbt-aspectj
addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.10.4")

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

resolvers += "repo.spray.io" at "http://repo.spray.io/"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"
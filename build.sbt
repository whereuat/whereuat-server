name := """whereuat-server"""

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "com.twilio.sdk" % "twilio-java-sdk" % "3.4.5",
  "org.mongodb" %% "casbah" % "3.1.0",
  "org.yaml" % "snakeyaml" % "1.17",
  "com.google.gcm" % "gcm-server" % "1.0.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

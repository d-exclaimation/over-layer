
// The simplest possible sbt build file is just one line:

scalaVersion := "2.13.6"

name := "over-layer"

ThisBuild / organization := "io.github.d-exclaimation"
ThisBuild / version := "1.0.3"
ThisBuild / organizationHomepage := Some(url("https://www.dexclaimation.com"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/d-exclaimation/over-layer"),
    "scm:git@github.d-exclaimation/over-layer.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "d-exclaimation",
    name = "Vincent",
    email = "thisoneis4business@gmail.com",
    url = url("https://www.dexclaimation.com")
  )
)

// crossPaths := false

ThisBuild / description := "A GraphQL over Websocket Stream-based Transport Layer on Akka."
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage := Some(url("https://overlayer.netlify.app"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ThisBuild / publishMavenStyle := true

ThisBuild / versionScheme := Some("early-semver")

libraryDependencies ++= {
  val sangriaVer = "3.0.0"
  val AkkaVersion = "2.6.18"
  val AkkaHttpVersion = "10.2.9"
  Seq(
    "org.sangria-graphql" %% "sangria" % sangriaVer,
    "org.sangria-graphql" %% "sangria-spray-json" % "1.0.2",
    "org.sangria-graphql" %% "sangria-akka-streams" % "1.0.2",
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  )
}


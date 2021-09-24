
// The simplest possible sbt build file is just one line:

scalaVersion := "2.13.3"

ThisBuild / name := "over-layer"
ThisBuild / organization := "io.github.d-exclaimation"
ThisBuild / version := "0.1.2"
ThisBuild / organizationHomepage := Some(url("https://www.dexclaimation.com"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/d-exclaimation/whiskey"),
    "scm:git@github.d-exclaimation/whiskey.git"
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

crossPaths := false

ThisBuild / description := "A GraphQL over Websocket Stream-based Subscription Transport Layer on Akka."
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage := Some(url("https://github.com/d-exclaimation/whiskey"))

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
  val sangriaVer = "2.1.3"
  val AkkaVersion = "2.6.16"
  val AkkaHttpVersion = "10.2.6"
  Seq(
    "org.sangria-graphql" %% "sangria" % sangriaVer,
    "org.sangria-graphql" %% "sangria-spray-json" % "1.0.2",
    "org.sangria-graphql" %% "sangria-akka-streams" % "1.0.2",
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  )
}

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

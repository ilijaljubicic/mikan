
name := "mikan"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

val akkaVersion = "2.4.17"
val reactivemongoVersion = "0.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.play.extras" %% "play-geojson" % "1.4.0",
  "org.reactivemongo" %% "play2-reactivemongo" % reactivemongoVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

routesGenerator := InjectedRoutesGenerator

val scalacOptions = Seq(
  "-Xlint",
  "-unchecked",
  "-deprecation",
  "-feature"
)

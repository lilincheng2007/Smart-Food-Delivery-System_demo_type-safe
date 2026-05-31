ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "com.delivery"
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / Compile / run / outputStrategy := Some(StdoutOutput)

val http4sV = "0.23.27"

lazy val commonLibDeps = Seq(
  "org.typelevel" %% "cats-effect" % "3.5.4",
  "org.http4s" %% "http4s-dsl" % http4sV,
  "org.http4s" %% "http4s-ember-server" % http4sV,
  "org.http4s" %% "http4s-circe" % http4sV,
  "io.circe" %% "circe-generic" % "0.14.9",
  "io.circe" %% "circe-parser" % "0.14.9",
  "com.github.jwt-scala" %% "jwt-circe" % "10.0.1",
  "com.zaxxer" % "HikariCP" % "5.1.0",
  "org.postgresql" % "postgresql" % "42.7.4",
  "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
  "org.slf4j" % "slf4j-simple" % "2.0.13",
  "org.http4s" %% "http4s-ember-client" % http4sV
)

lazy val root = (project in file("."))
  .settings(
    name := "delivery-backend",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src",
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    libraryDependencies ++= commonLibDeps,
    Compile / run / mainClass := Some("delivery.Main"),
    Compile / run / fork := true,
    publish / skip := true
  )

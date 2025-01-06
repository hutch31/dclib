
ThisBuild / organization := "org.ghutchis"
ThisBuild / version := "2024-12-19"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / publish / skip := true
ThisBuild / publishMavenStyle := true
//ThisBuild / versionScheme := Some("early-semver")

val chiselVersion = "6.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "dclib",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "org.scalatest" % "scalatest_2.13" % "3.2.19"
      //"edu.berkeley.cs" %% "chiseltest" % "0.5.1" % "test"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
    fork := true,
    publish / skip := false
  )

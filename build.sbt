ThisBuild / organization := "org.ghutchis"
ThisBuild / version := "2025-01-06"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / publish / skip := true
ThisBuild / publishMavenStyle := true

val chiselVersion = "3.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "dclib",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.2" % "test",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    fork := true,
    publish / skip := false
  )

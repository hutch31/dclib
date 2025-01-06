// Automatically pick up tags and parse them to determine the current code version
// Copied from: https://alterationx10.com/2022/05/26/publish-to-github
// as imitation is the sincerest form of flattery
val tagWithQualifier: String => String => String =
  qualifier =>
    tagVersion => s"%s.%s.%s-${qualifier}%s".format(tagVersion.split("\\."): _*)

val tagAlpha: String => String = tagWithQualifier("a")
val tagBeta: String => String = tagWithQualifier("b")
val tagMilestone: String => String = tagWithQualifier("m")
val tagRC: String => String = tagWithQualifier("rc")

val defaultVersion: String = "0.0.0-a0"
val versionFromTag: String = sys.env
  .get("GITHUB_REF_TYPE")
  .filter(_ == "tag")
  .flatMap(_ => sys.env.get("GITHUB_REF_NAME"))
  .flatMap { t =>
    t.headOption.map {
      case 'a' => tagAlpha(t.tail) // Alpha build, a1.2.3.4
      case 'b' => tagBeta(t.tail) // Beta build, b1.2.3.4
      case 'm' => tagMilestone(t.tail) // Milestone build, m1.2.3.4
      case 'r' => tagRC(t.tail) // RC build, r1.2.3.4
      case 'v' => t.tail // Production build, should be v1.2.3
      case _ => defaultVersion
    }
  }
  .getOrElse(defaultVersion)

ThisBuild / organization := "org.ghutchis"
ThisBuild / version := versionFromTag
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / publish / skip := true
ThisBuild / publishMavenStyle := true
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / publishTo := Some(
  "GitHub Package Registry " at "https://maven.pkg.github.com/hutch31/dclib"
)
ThisBuild / credentials += Credentials(
  "GitHub Package Registry", // realm
  "maven.pkg.github.com", // host
  "hutch31", // user
  sys.env.getOrElse("GITHUB_TOKEN", "abc123") // password
)

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

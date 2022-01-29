val scalaVersion_3 = "3.1.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dotty-compiler-example",
    organization := "com.github.tototoshi",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scalaVersion_3,
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
      "io.get-coursier" % "interface" % "1.0.6",
      "org.scalatest" %% "scalatest" % "3.2.11" % "test"
    ),
run / fork := true,
  )

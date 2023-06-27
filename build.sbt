val scala3Version = "3.3.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "gossip-glomers",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "com.bilal-fazlani" %% "zio-maelstrom" % "0.2.0"
    )
  )

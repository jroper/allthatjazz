// Static site generator for the blog in the parent directory. Derived from the ERQX blog engine.
lazy val root = (project in file("."))
  .enablePlugins(SbtTwirl, SbtWeb)

name := "static-blog-engine"
organization := "au.id.jazzy.erqx"
version := "3.0.0"

scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
  "org.playframework" %% "play-doc" % "3.0.1",
  "org.yaml" % "snakeyaml" % "2.6",
  "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

// Theme web assets
libraryDependencies ++= Seq(
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.webjars" % "prettify" % "4-Mar-2013",
  "org.webjars" % "retinajs" % "0.0.2"
)

TwirlKeys.templateImports ++= Seq("au.id.jazzy.erqx.engine.models._", "au.id.jazzy.erqx.engine.site._")

LessKeys.compress := true

Concat.groups := Seq(
  "erqx-jazzy-theme.css" -> group(Seq("lib/bootstrap/css/bootstrap.min.css", "main.min.css", "lib/prettify/prettify.css")),
  "erqx-jazzy-theme.js" -> group(Seq("lib/jquery/jquery.min.js", "lib/prettify/prettify.js", "lib/prettify/lang-scala.js", "lib/retinajs/retina.js"))
)

Assets / pipelineStages := Seq(concat)

// The generator copies the theme assets from the web stage directory, so make sure they're built before running
Compile / run := (Compile / run).dependsOn(WebKeys.stage).evaluated
Compile / run / fork := true

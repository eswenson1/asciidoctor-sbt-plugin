organization := "pl.project13.scala"

name := "simple-doc"

version := "2.3.8"

scalaVersion := "2.11.4"

logBuffered := false

// docs settings

options in AsciiDoctor += ("title-suffix" -> "Akka Dodumentation")

options in AsciiDoctor += ("html-layout.vars" -> List(
  "{{akka.version.current}}" -> version.value
))

// dependencies

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.8"

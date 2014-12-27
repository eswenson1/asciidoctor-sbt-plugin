organization := "pl.project13.scala"

name := "simple-doc"

version := "0.0.1"

scalaVersion := "2.11.4"

logBuffered := false

// docs settings

options in AsciiDoctor += ("title-suffix" -> "Akka Dodumentation")

options in AsciiDoctor += ("html-layout.vars" -> List(
  "{{akka.version.current}}" -> "2.3.8"
))

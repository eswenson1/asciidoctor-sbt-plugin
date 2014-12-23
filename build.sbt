import bintray.Keys._

sbtPlugin := true

organization := "pl.project13.scala"

name := "sbt-asciidoctor"

version := "0.0.1"

scalaVersion := "2.10.4"

libraryDependencies += "org.asciidoctor" % "asciidoctorj" % "1.5.2"

libraryDependencies += "com.google.guava" % "guava" % "18.0"

publishTo <<= isSnapshot { snapshot =>
  if (snapshot) Some(Classpaths.sbtPluginSnapshots) else Some(Classpaths.sbtPluginReleases)
}

// publishing settings

publishMavenStyle := false

bintrayPublishSettings

repository in bintray := "sbt-plugins"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := None

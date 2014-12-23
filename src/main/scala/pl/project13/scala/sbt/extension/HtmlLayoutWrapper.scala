package pl.project13.scala.sbt.extension

import java.io.File

import org.asciidoctor.ast.Document
import org.asciidoctor.extension.Postprocessor

class HtmlLayoutWrapper(config: java.util.HashMap[String, AnyRef]) extends Postprocessor(config) {

  val file = new File(Option(config.get("html-layout.file")).getOrElse("src/adoc/layout.html").toString)
  require(file.exists(), s"File ${file.getAbsolutePath} required by ${getClass.getName} does not exist!")

  val bodyMarker = Option(config.get("html-layout.body-marker")).getOrElse("{{body}}").toString

  override def process(document: Document, input: String): String = if (document.basebackend("html")) {
    val template = io.Source.fromFile(file).getLines().mkString("\n")
    template.replace(bodyMarker, input)
  } else {
    println("NOT NOT NOT executing wrapper!!!!!! = ")
    input
  }

}

object HtmlLayoutWrapper {
  var layout: String = "{{body}}"
}
package pl.project13.scala.sbt.extension

import java.io.File
import java.util.concurrent.atomic.AtomicReference

import org.asciidoctor.ast.Document
import org.asciidoctor.extension.Postprocessor

class HtmlLayoutWrapper(opts: java.util.HashMap[String, AnyRef]) extends Postprocessor(opts) {

  private val filename = Option(opts.get("html-layout.file")).getOrElse("src/adoc/layout.html").toString

  lazy val file = new File(filename)

  val template: String = {
    def loadTemplate() = {
      require(file.exists(), s"File ${file.getAbsolutePath} required by ${getClass.getName} does not exist!")
      val source = io.Source.fromFile(file)
      try source.getLines().mkString("\n") finally source.close()
    }

    HtmlLayoutWrapper.layout.get() match {
      case (`filename`, t) =>
        println("Template cache hit!")
        t

      case null =>
        val t = loadTemplate()
        HtmlLayoutWrapper.layout.compareAndSet(null, filename -> t)
        t
      case key @ (name, oldTemplate) =>
        val t = loadTemplate()
        HtmlLayoutWrapper.layout.compareAndSet(key, filename -> t)
        t
    }
  }

  val bodyMarker = Option(opts.get("html-layout.body-marker")).getOrElse("{{body}}").toString
  val titleMarker = Option(opts.get("html-layout.title-marker")).getOrElse("{{title}}").toString

  override def process(document: Document, input: String): String = if (document.basebackend("html")) {
    template
      .replace(bodyMarker, input)
      .replace(titleMarker, Option(document.doctitle()).getOrElse("[no title]"))
      .replaceAll("assets/", opts.get("assets-rel-path").toString)
  } else input

}

object HtmlLayoutWrapper {
  var layout: AtomicReference[(String, String)] = new AtomicReference[(String, String)]()
}
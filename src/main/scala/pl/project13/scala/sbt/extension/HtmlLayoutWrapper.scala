package pl.project13.scala.sbt.extension

import java.io.File
import java.util.concurrent.atomic.AtomicReference

import org.asciidoctor.ast.Document
import org.asciidoctor.extension.Postprocessor

class HtmlLayoutWrapper(opts: java.util.HashMap[String, AnyRef]) extends Postprocessor(opts) {

  private val filename = Option(opts.get("html-layout.file")).getOrElse("src/adoc/layout.html").toString
  private val bodyMarker = Option(opts.get("html-layout.body-marker")).getOrElse("""\{\{body\}\}""").toString
  private val titleMarker = Option(opts.get("html-layout.title-marker")).getOrElse("""\{\{title\}\}""").toString

  private val assetsRelPath = Option(opts.get("assets-rel-path")).getOrElse("assets/")
  private val titleSuffix: List[String] = Option(opts.get("title-suffix")).map(_.toString :: Nil).getOrElse(Nil)

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

  override def process(document: Document, input: String): String = if (document.basebackend("html")) {
    val docTitle = Option(document.doctitle()).getOrElse("")
    template
      .replaceAll(bodyMarker, input)
      .replaceAll(titleMarker, (docTitle :: titleSuffix).mkString(" – "))
      .replaceAll("assets/", assetsRelPath.toString) // this is a hack, because Processors are unaware which file they work on
  } else input

}

object HtmlLayoutWrapper {
  var layout: AtomicReference[(String, String)] = new AtomicReference[(String, String)]()
}
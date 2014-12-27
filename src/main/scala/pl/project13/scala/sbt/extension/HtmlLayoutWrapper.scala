package pl.project13.scala.sbt.extension

import java.util
import java.io.File
import java.util.concurrent.atomic.AtomicReference

import org.asciidoctor.ast.Document
import org.asciidoctor.extension.Postprocessor

/**
 * Vars:
 *
 * {{{
 *   html-layout.file
 *   html-layout.body-marker
 *   html-layout.head-title-marker
 *   html-layout.title-marker
 *
 *   html-layout.vars.*
 * }}}
 */
class HtmlLayoutWrapper(opts: java.util.Map[String, AnyRef]) extends Postprocessor(opts) {

  private val filename = Option(opts.get("html-layout.file")).getOrElse("src/adoc/layout.html").toString
  private val bodyMarker = Option(opts.get("html-layout.body-marker")).getOrElse("""{{body}}""").toString
  private val headTitleMarker = Option(opts.get("html-layout.head-title-marker")).getOrElse("""{{head.title}}""").toString
  private val titleMarker = Option(opts.get("html-layout.title-marker")).getOrElse("""{{title}}""").toString

  def assetsRelPath(opts: util.Map[AnyRef, AnyRef]): String = Option(opts.get("assets-rel-path")).map(_.toString).getOrElse("assets/")
  private val titleSuffix: String = Option(opts.get("title-suffix")).map(" – " + _.toString).getOrElse("")

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

    val phase2 = template
      .replaceAll("""assets/""", assetsRelPath(document.getOptions)) // this is a hack, because Processors are unaware which file they work on
      .replace(bodyMarker, input)
      .replace(headTitleMarker, s"$docTitle$titleSuffix")
      .replace(titleMarker, docTitle)

    // replace user provided variables
    val userVars = Option(opts.get("html-layout.vars")).map(_.asInstanceOf[List[(String, String)]]).getOrElse(Nil)
    userVars.foldLeft(phase2) {
      case (acc, (k, v)) => phase2.replace(k, v)
    }
  } else input

}

object HtmlLayoutWrapper {
  var layout: AtomicReference[(String, String)] = new AtomicReference[(String, String)]()
}
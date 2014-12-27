package pl.project13.scala.sbt.extension

import org.asciidoctor.ast.AbstractBlock
import org.asciidoctor.extension.InlineMacroProcessor
import java.util
import scala.collection.JavaConversions._

class ScaladocInlineMacro(name: String, opts: util.Map[String, AnyRef]) extends InlineMacroProcessor(name, opts) {

  val CodeTicksPattern = """`(.*)`""".r

  override def process(parent: AbstractBlock, target: String, attrs: util.Map[String, AnyRef]): AnyRef = {
    val version = "2.3.8" // todo get from options
    val (displayName, realName) = resolveName(target, parent.document().attributes())
    val url = s"http://doc.akka.io/api/akka/$version/#$realName" // scaladoc

    s"""<a href="$url" class="scaladoc-link" title="$realName"><code>$displayName</code></a>"""
  }

  def resolveName(target: String, attrs: util.Map[String, AnyRef]): (String, String) = {
    val name = target match {
      case CodeTicksPattern(clazzName) => clazzName
      case clazzName => clazzName
    }

    val isQualified = name contains "."
    val fullName = if (isQualified) name else resolveWithImports(name, attrs)

    val displayName = if (name.endsWith("$")) name.dropRight(1) else name
    displayName -> fullName
  }

  def resolveWithImports(simpleName: String, attrs: util.Map[String, AnyRef]): String = {
    val imports = attrs.filter(_._1.startsWith("scaladoc-import")).map(_._2.toString)

    // direct match?
    val directlyImported = imports.find(_.endsWith(simpleName))

    directlyImported getOrElse {
      // wild card imported?

      require(imports.nonEmpty, s"No :scaladoc-import_: given, and unable to identify class: [$simpleName]! Please disambiguate in-line or add imports (:scaladoc-import_1: akka.actor._).")

      // TODO should check if the class really exists, and pick the right import
      imports.head.replace("_", simpleName)
    }
  }
}
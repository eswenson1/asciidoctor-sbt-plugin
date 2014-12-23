package pl.project13.scala.sbt

import java.io.{BufferedReader, FileReader, FileWriter}
import java.util

import org.asciidoctor.AsciiDocDirectoryWalker
import org.asciidoctor.extension._
import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._
import scala.collection.immutable

object SbtAsciidoctor extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {
    val AsciiDoctor = config("asciidoctor") extend Compile

    val generate = taskKey[Seq[File]]("Generate benchmark asciidoctor Java code")

    val outputTarget = settingKey[File]("Directory where the bytecode to be consumed and generated sources should be written to (`target` or sometimes `target/scala-2.10`)")

    val stylesDir = settingKey[File]("Stylesheets directory")

    val stylesheet = settingKey[File]("stylesheet (e.g. main.css)")

    val options = settingKey[util.HashMap[String, AnyRef]]("Options for AsciiDoctor")

    val extensions = settingKey[immutable.Seq[Class[_ <: Processor]]]("Options for AsciiDoctor")
  }
  import pl.project13.scala.sbt.SbtAsciidoctor.autoImport._

  private lazy val engine = org.asciidoctor.Asciidoctor.Factory.create(getClass.getClassLoader)


  lazy val asciidoctorSettings = Seq(
    resourceGenerators in Compile += (generate in AsciiDoctor).taskValue,

    stylesDir in AsciiDoctor := baseDirectory.value / "assets" / "stylesheets",
    stylesheet in AsciiDoctor := (stylesDir in AsciiDoctor).value / "main.css",

    options in AsciiDoctor := {
      val opts = new util.HashMap[String, AnyRef]()
      opts.put("stylesdir", (stylesDir in AsciiDoctor).value)
      opts.put("stylesheet", (stylesheet in AsciiDoctor).value)
      opts
    },

    extensions in AsciiDoctor := List(
      classOf[extension.HtmlLayoutWrapper]
    ),

    generate in AsciiDoctor := {
      val log = streams.value.log
      val opts = (options in AsciiDoctor).value
      val extns = (extensions in AsciiDoctor).value

      val extensionRegistry = engine.javaExtensionRegistry
      extns.map(_.getConstructor(classOf[util.HashMap[_, _]]).newInstance(opts)) collect {
        case p: Preprocessor         => extensionRegistry.preprocessor(p)
        case p: BlockProcessor       => extensionRegistry.block(p)
        case p: DocinfoProcessor     => extensionRegistry.docinfoProcessor(p)
        case p: IncludeProcessor     => extensionRegistry.includeProcessor(p)
        case p: InlineMacroProcessor => extensionRegistry.inlineMacro(p)
        case p: Treeprocessor        => extensionRegistry.treeprocessor(p)
        case p: BlockMacroProcessor  => extensionRegistry.blockMacro(p)
        case p: Postprocessor        =>

//          val rubyRuntimeField = extensionRegistry.getClass.getDeclaredField("rubyRuntime")
//          rubyRuntimeField.setAccessible(true)
//          val ruby = rubyRuntimeField.get(extensionRegistry).asInstanceOf[Ruby]
//
//          val asciidoctorModuleField = extensionRegistry.getClass.getDeclaredField("asciidoctorModule")
//          asciidoctorModuleField.setAccessible(true)
//          val module = asciidoctorModuleField.get(extensionRegistry).asInstanceOf[AsciidoctorModule]
//
//          ruby.evalScriptlet("java_import '" + p.getClass.getCanonicalName + "'")
//          module.postprocessor(p)
          extensionRegistry.postprocessor(p.getClass)
        case x                       => throw new Exception(s"Given extension [$x] is of unknown type! Unable to register as processor...")
      }

      val docsWalker = new AsciiDocDirectoryWalker("src/adoc")

      val files = docsWalker.scan() map { adoc =>
        log.warn("adoc = " + adoc)

        val output = new File(adoc + ".html")

        streams.value.log.warn("Options = " + opts)

        writeToFile(opts, adoc, output)
      }
      log.warn("files = " + files.toList)
      files.toSeq
    }
  )

  private def writeToFile(options: util.HashMap[String, AnyRef], adocFile: File, outFile: File): File = {
    val reader = new BufferedReader(new FileReader(adocFile))
    val writer = new FileWriter(outFile)
    try {
      engine.convert(reader, writer, options)
      outFile
    } finally {
      reader.close()
      writer.close()
    }
  }

  override lazy val projectSettings = inConfig(Compile)(asciidoctorSettings)

}

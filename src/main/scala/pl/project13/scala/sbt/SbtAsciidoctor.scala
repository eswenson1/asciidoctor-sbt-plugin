package pl.project13.scala.sbt

import java.io.{BufferedReader, FileReader, FileWriter}
import java.util

import com.github.sommeri.less4j.utils.URIUtils
import org.asciidoctor.AsciiDocDirectoryWalker
import org.asciidoctor.extension._
import org.asciidoctor.internal.AsciidoctorModule
import org.jruby.Ruby
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

    val assetsDir = settingKey[File]("Assets directory")
    val stylesDir = settingKey[File]("Stylesheets directory (in assets dir)")
    val stylesheet = settingKey[File]("stylesheet (e.g. main.css)")
    val adocDir = settingKey[File]("Asciidoctor source directory")

    val options = settingKey[util.HashMap[String, AnyRef]]("Options for AsciiDoctor")

    val extensions = settingKey[immutable.Seq[Class[_ <: Processor]]]("Options for AsciiDoctor")
  }
  import pl.project13.scala.sbt.SbtAsciidoctor.autoImport._

  private lazy val engine = org.asciidoctor.Asciidoctor.Factory.create(getClass.getClassLoader)


  lazy val asciidoctorSettings = Seq(
    resourceGenerators in Compile += (generate in AsciiDoctor).taskValue,

    adocDir in AsciiDoctor := baseDirectory.value / "src" / "adoc",

    assetsDir in AsciiDoctor := (adocDir in AsciiDoctor).value / "assets",
    stylesDir in AsciiDoctor := (assetsDir in AsciiDoctor).value / "stylesheets",
    stylesheet in AsciiDoctor := (stylesDir in AsciiDoctor).value / "main.css",

    options in AsciiDoctor := {
      val opts = new util.HashMap[String, AnyRef]()
      opts.put("assetsdir", (assetsDir in AsciiDoctor).value)
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

      registerExtensions(opts, engine.javaExtensionRegistry, extns)

      val docsWalker = new AsciiDocDirectoryWalker((adocDir in AsciiDoctor).value.getAbsolutePath)

      val files = docsWalker.scan() map { adoc =>
        log.info("adoc = " + adoc)

        opts.put("assets-rel-path", URIUtils.relativize(adoc, (assetsDir in AsciiDoctor).value))

        writeToFile(opts, adoc, new File(adoc + ".html"))
      }
      files.toSeq
    }
  )

  /**
   * Ugly hacks ahead, see: https://github.com/asciidoctor/asciidoctorj/issues/250
   */
  private def registerExtensions(opts: util.HashMap[String, AnyRef], extensionRegistry: JavaExtensionRegistry,
                                 extensions: immutable.Seq[Class[_ <: Processor]]) = {
    val extensionInstances = extensions.map(_.getConstructor(classOf[util.HashMap[_, _]]).newInstance(opts))

    // begining of hack area
    // FIXME: Works around issue in asciidoctorj: https://github.com/asciidoctor/asciidoctorj/issues/250
    val rubyRuntimeField = extensionRegistry.getClass.getDeclaredField("rubyRuntime")
    rubyRuntimeField.setAccessible(true)
    val ruby = rubyRuntimeField.get(extensionRegistry).asInstanceOf[Ruby]

    val asciidoctorModuleField = extensionRegistry.getClass.getDeclaredField("asciidoctorModule")
    asciidoctorModuleField.setAccessible(true)
    val asciidoctorModule = asciidoctorModuleField.get(extensionRegistry).asInstanceOf[AsciidoctorModule]

    // FIXME: Works around issue in asciidoctorj: https://github.com/asciidoctor/asciidoctorj/issues/250
    def rubyImport[T](p: T): T = {
      ruby.evalScriptlet(s"java_import '${p.getClass.getCanonicalName}'")
      p
    }

    // end of hack area

    // FIXME: below, use extensionRegistry once #250 is fixed
    extensionInstances.map(rubyImport) collect {
      case p: Preprocessor         => asciidoctorModule.preprocessor(p)
      case p: BlockProcessor       => asciidoctorModule.block_processor(p.getName, p)
      case p: DocinfoProcessor     => asciidoctorModule.docinfo_processor(p)
      case p: IncludeProcessor     => asciidoctorModule.include_processor(p)
      case p: InlineMacroProcessor => asciidoctorModule.inline_macro(p.getName, p)
      case p: Treeprocessor        => asciidoctorModule.treeprocessor(p)
      case p: BlockMacroProcessor  => asciidoctorModule.block_macro(p.getName, p)
      case p: Postprocessor        => asciidoctorModule.postprocessor(p)
      case x                       => throw new Exception(s"Given extension [$x] is of unknown type! Unable to register as processor...")
    }

  }

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

package pl.project13.scala.sbt

import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.util

import com.github.sommeri.less4j.utils.URIUtils
import org.asciidoctor.AsciiDocDirectoryWalker
import org.asciidoctor.extension._
import org.asciidoctor.internal.AsciidoctorModule
import org.jruby.Ruby
import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
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

    val templatesDirs = settingKey[immutable.Seq[File]]("Asciidoctor templates directory - http://asciidoctor.org/docs/user-manual/#provide-custom-templates")

    val options = settingKey[Map[String, AnyRef]]("Options for AsciiDoctor")

    val extensions = settingKey[immutable.Seq[ProcessorRegistration]]("Extensions for AsciiDoctor")

    final case class ProcessorRegistration(name: String, clazz: Class[_ <: Processor])
    implicit def clazz2Preprocessor(clazz: Class[_ <: Preprocessor]): ProcessorRegistration = ProcessorRegistration(null, clazz)
    implicit def clazz2DocinfoProcessor(clazz: Class[_ <: DocinfoProcessor]): ProcessorRegistration = ProcessorRegistration(null, clazz)
    implicit def clazz2IncludeProcessor(clazz: Class[_ <: IncludeProcessor]): ProcessorRegistration = ProcessorRegistration(null, clazz)
    implicit def clazz2Treeprocessor(clazz: Class[_ <: Treeprocessor]): ProcessorRegistration = ProcessorRegistration(null, clazz)
    implicit def clazz2Postprocessor(clazz: Class[_ <: Postprocessor]): ProcessorRegistration = ProcessorRegistration(null, clazz)
    implicit def clazz2BlockProcessor(p: (String, Class[_ <: BlockProcessor])): ProcessorRegistration = ProcessorRegistration(p._1, p._2)
    implicit def clazz2BlockMacroProcessor(p: (String, Class[_ <: BlockMacroProcessor])): ProcessorRegistration = ProcessorRegistration(p._1, p._2)
    implicit def clazz2InlineMacroProcessor(p: (String, Class[_ <: InlineMacroProcessor])): ProcessorRegistration = ProcessorRegistration(p._1, p._2)
  }
  import pl.project13.scala.sbt.SbtAsciidoctor.autoImport._

  private lazy val engine = org.asciidoctor.Asciidoctor.Factory.create(getClass.getClassLoader)


  lazy val asciidoctorSettings = Seq(
    resourceGenerators in Compile += (generate in AsciiDoctor).taskValue,

    adocDir in AsciiDoctor := baseDirectory.value / "src" / "adoc",

    assetsDir in AsciiDoctor := (adocDir in AsciiDoctor).value / "assets",
    stylesDir in AsciiDoctor := (assetsDir in AsciiDoctor).value / "stylesheets",
    templatesDirs in AsciiDoctor := (adocDir in AsciiDoctor).value / "templates" :: Nil,
    stylesheet in AsciiDoctor := (stylesDir in AsciiDoctor).value / "main.css",

    options in AsciiDoctor := {
      import org.asciidoctor.Options._
      Map(
        "assetsdir" -> (assetsDir in AsciiDoctor).value,
        "stylesdir" -> (stylesDir in AsciiDoctor).value,
        "stylesheet" -> (stylesheet in AsciiDoctor).value,
        TEMPLATE_DIRS -> (templatesDirs in AsciiDoctor).value.map(_.getAbsolutePath).asJava
      )
    },

    extensions in AsciiDoctor := List(
      classOf[extension.HtmlLayoutWrapper],
      "class" -> classOf[extension.ScaladocInlineMacro]
    ),

    generate in AsciiDoctor := {
      val log = streams.value.log
      val mutableOpts: util.HashMap[String, AnyRef] = {
         // asciidoctors API requires the HashMap explicitly
        val m = new util.HashMap[String, AnyRef]()
        (options in AsciiDoctor).value foreach { case (k, v) => m.put(k, v) }
        m
      }
      val extns = (extensions in AsciiDoctor).value

      log.info(s"[sbt-asciidoctor] Loading extensions: ${extns.map(_.clazz.getName).mkString(", ")}")
      registerExtensions(mutableOpts, engine.javaExtensionRegistry, extns)

      val docsWalker = new AsciiDocDirectoryWalker((adocDir in AsciiDoctor).value.getAbsolutePath)

      val files = docsWalker.scan() map { adoc =>
        val target = new File(adoc.getAbsolutePath.replace(".adoc", ".html"))
        log.info(s"Generating: $target ...")

        // todo expose immutable here
        mutableOpts.put("assets-rel-path", URIUtils.relativize(target, (assetsDir in AsciiDoctor).value))

        writeToFile(mutableOpts, adoc, target)
      }
      files.toSeq
    }
  )

  /**
   * Ugly hacks ahead, see: https://github.com/asciidoctor/asciidoctorj/issues/250
   */
  private def registerExtensions(opts: util.HashMap[String, AnyRef], extensionRegistry: JavaExtensionRegistry,
                                 extensions: immutable.Seq[ProcessorRegistration]) = {
    val extensionInstances = extensions.map { ex =>
      ex.name match {
        case null => ex.clazz.getConstructor(classOf[util.Map[_, _]]).newInstance(opts)
        case name: String => ex.clazz.getConstructor(classOf[String], classOf[util.Map[_, _]]).newInstance(name, opts)
      }
    }

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
      case p: InlineMacroProcessor => asciidoctorModule.inline_macro(p, p.getName)
      case p: BlockProcessor       => asciidoctorModule.block_processor(p, p.getName)
      case p: BlockMacroProcessor  => asciidoctorModule.block_macro(p, p.getName)
      case p: DocinfoProcessor     => asciidoctorModule.docinfo_processor(p)
      case p: IncludeProcessor     => asciidoctorModule.include_processor(p)
      case p: Treeprocessor        => asciidoctorModule.treeprocessor(p)
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

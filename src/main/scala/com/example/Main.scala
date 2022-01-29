package com.example

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Path, Paths}
import dotty.tools.dotc.core.Contexts, Contexts.{ Context, ctx }
import dotty.tools.dotc.{Compiler, Driver}
import dotty.tools.io.{ PlainDirectory, Directory, ClassPath }
import coursierapi.{Dependency, Fetch}
import scala.jdk.CollectionConverters._

/**
  * Reference:
  * https://github.com/lampepfl/dotty/blob/d871d35b91ea9a56341676921df14c60dee62ac7/compiler/src/dotty/tools/scripting/ScriptingDriver.scala
  */
object Main {

  def main(args: Array[String]): Unit = {
    val code = """object Calculator { def sum(i: Int, j: Int): Int = i + j }"""

    val fetch = Fetch.create()

    fetch.addDependencies(
      Dependency.of("org.scala-lang", "scala3-library_3", "3.1.1")
    )

    val classpath = fetch
      .fetch()
      .asScala
      .map(_.toPath())
      .mkString(":")

    val compilerArgs = Array("-classpath", classpath)

    withTempFile { file =>
      Files.write(file, code.getBytes)
      withTempDirectory[Unit] { dir =>
        val driver = new TestDriver(dir, compilerArgs, file)
        val cl = driver.compileAndLoad()

        val cls = cl.loadClass("Calculator")
        val answer = cls.getMethod("sum", classOf[Int], classOf[Int]).invoke(null, 20, 22)
        println(answer)
        assert(answer == 42)
      }
    }

  }

  private def withTempDirectory[A](f: Path => A): A = {
    val p = Files.createTempDirectory("compile-and-run")
    p.toFile.deleteOnExit()

    try {
      f(p)
    } finally {
      p.toFile.listFiles.foreach(_.delete())
    }
  }

  private def withTempFile[A](f: Path => A): A = {
    val file = File.createTempFile("compile-and-run", ".scala")
    file.deleteOnExit()
    f(file.toPath)
  }

}

class TestDriver(outDir: Path, compilerArgs: Array[String], path: Path) extends Driver {
  def compileAndLoad(): ClassLoader = {
    val Some((toCompile, rootCtx)) = setup(compilerArgs :+ path.toAbsolutePath.toString, initCtx.fresh)

    given Context = rootCtx.fresh.setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir)))

    doCompile(newCompiler, toCompile)

    val classpath = s"${ctx.settings.classpath.value}${sys.props("path.separator")}${sys.props("java.class.path")}"
    val classpathEntries = ClassPath.expandPath(classpath, expandStar=true).map { Paths.get(_) }
    val classpathUrls = (classpathEntries :+ outDir).map { _.toUri.toURL }
    new URLClassLoader(classpathUrls.toArray)
  }

}

package play.scalasupport.compiler

import com.typesafe.config._
import com.typesafe.zinc._
import java.io.File
import xsbti.compile.CompileOrder
import sbt.inc.Analysis
import play.Logger
import play.Play
import xsbti.{ Logger => XsbtiLogger }

class PlayScalaZincCompiler(app: File, libs: File, classpath: List[File], output: File, skipResult: Boolean = false) {

    final val SCALA_HOME = "scala"
    final val LIBS_HOME = "lib"
    final val SCALA_COMPILER_JAR = new File(SCALA_HOME, "scala-compiler.jar")
    final val SCALA_LIBRARY_JAR = new File(SCALA_HOME, "scala-library.jar")
    final val SBT_INTERFACE_JAR = new File(SCALA_HOME, "sbt-interface")
    final val COMPILER_INTERFACE_SRC_JAR = new File(SCALA_HOME, "compiler-interface-sources.jar")
    final val EXTRA_JARS = Array("akka-actors.jar", "scala-actors.jar", "scala-actors-migration.jar", "scala-reflect.jar") map (f => new File(SCALA_HOME, f))
    final val ZINC_CACHE_DIR = new File(Play.tmpDir,"zinc_cache_dir")
    
    val setup = Setup.setup(SCALA_COMPILER_JAR, SCALA_LIBRARY_JAR, EXTRA_JARS, SBT_INTERFACE_JAR, COMPILER_INTERFACE_SRC_JAR, None, false)
    
    val inputs = Inputs.inputs(classpath, sources, classesDirectory, scalacOptions, javacOptions, analysisCache, analysisCacheMap, forceClean, javaOnly, compileOrder, incOptions, outputRelations, outputProducts, mirrorAnalysis)
    
    def update(sources: List[File]): Either[CompilationError, (List[ClassDefinition], List[ClassDefinition])] = {

        return Right(Nil, Nil)
    }

}

object PlayScalaZincCompiler {
    
    

}

class ZincLogger extends XsbtiLogger {
    // TODO : define a proper strategy in logback.xml
    def error(arg: xsbti.F0[String]) = {
        println("->ERROR: " + arg.apply())
        Logger.error(arg.apply)
    }
    def warn(arg: xsbti.F0[String]) = {
        println("->WARN: " + arg.apply())
        Logger.warn(arg.apply)
    }
    def info(arg: xsbti.F0[String]) = {
        println("->INFO: " + arg.apply())
        Logger.info(arg.apply)
    }
    def debug(arg: xsbti.F0[String]) = {
        println("->DEBUG: " + arg.apply())
        Logger.debug(arg.apply)
    }
    def trace(arg: xsbti.F0[Throwable]) = {
        println("->TRACE: " + arg.apply())
        Logger.trace("Compilation Exception:", arg.apply)
    }
}
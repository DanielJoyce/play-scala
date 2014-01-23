package play.scalasupport.compiler

import com.typesafe.config._
import com.typesafe.zinc._
import java.io.File
import xsbti.compile.CompileOrder

class PlayScalaZincCompiler(app: File, libs: File, classpath: List[File], output: File, skipResult: Boolean = false) {

    def update(sources: List[File]): Either[CompilationError, (List[ClassDefinition], List[ClassDefinition])] = {
        return Right(Nil, Nil)
    }

}
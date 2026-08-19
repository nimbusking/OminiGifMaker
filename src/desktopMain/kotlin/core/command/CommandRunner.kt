package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineExtractor
import com.ominigifmaker.core.engine.ProcessResult
import com.ominigifmaker.core.engine.ProcessRunner

/** 执行一条引擎命令：确保引擎已解压后交给 [ProcessRunner] 运行。 */
object CommandRunner {
    suspend fun run(command: EngineCommand): ProcessResult {
        EngineExtractor.ensureExtracted(command.engine)
        return ProcessRunner.run(
            executable = EngineExtractor.executableFile(command.engine),
            args = command.args,
        )
    }
}

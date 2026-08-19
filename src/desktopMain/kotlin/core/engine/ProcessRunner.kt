package com.ominigifmaker.core.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

/** 子进程执行结果：退出码 + 标准输出 + 标准错误。 */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * 引擎子进程调用封装。
 *
 * - 在 [Dispatchers.IO] 中执行 `ProcessBuilder.start()` 与 `waitFor()`，避免阻塞 UI 线程。
 * - 以 `List<String>` 传参，避免字符串拼接导致的注入问题。
 * - 并发读取标准输出与标准错误流，避免缓冲区写满导致进程挂起（死锁）。
 */
object ProcessRunner {

    suspend fun run(
        executable: File,
        args: List<String>,
        workingDirectory: File? = null,
    ): ProcessResult = withContext(Dispatchers.IO) {
        val command = buildList {
            add(executable.absolutePath)
            addAll(args)
        }
        val process = ProcessBuilder(command).apply {
            if (workingDirectory != null) directory(workingDirectory)
        }.start()

        val stdoutDeferred = async(Dispatchers.IO) {
            process.inputStream.bufferedReader().use { it.readText() }
        }
        val stderrDeferred = async(Dispatchers.IO) {
            process.errorStream.bufferedReader().use { it.readText() }
        }

        val exitCode = process.waitFor()
        ProcessResult(
            exitCode = exitCode,
            stdout = stdoutDeferred.await(),
            stderr = stderrDeferred.await(),
        )
    }
}

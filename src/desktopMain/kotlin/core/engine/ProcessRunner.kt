package com.ominigifmaker.core.engine

import com.ominigifmaker.core.utils.OsType
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
 * - 注入引擎运行时所需的环境变量（配置路径 + 动态库搜索路径，见 [configureEnvironment]）。
 */
object ProcessRunner {

    private val os: OsType = OsType.detect()

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
            configureEnvironment(executable)
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

    /**
     * 注入引擎运行时所需的环境变量。
     *
     * - `MAGICK_CONFIGURE_PATH`：ImageMagick 的静态/便携构建默认配置路径不指向自身目录，
     *   显式指向解压目录以便读取同目录的 `*.xml` / `sRGB.icc` 配置（三平台统一）。
     * - 动态库搜索路径：Windows 会把可执行文件同目录纳入 DLL 搜索路径；macOS / Linux
     *   默认不搜索，需显式通过 `DYLD_LIBRARY_PATH`（macOS，另加 `DYLD_FALLBACK_LIBRARY_PATH`
     *   兜底）/ `LD_LIBRARY_PATH`（Linux）指向解压目录，否则 libvips 等动态链接引擎
     *   找不到其依赖库。
     */
    private fun ProcessBuilder.configureEnvironment(executable: File) {
        val libDir = executable.absoluteFile.parentFile?.absolutePath ?: return
        environment()["MAGICK_CONFIGURE_PATH"] = libDir
        if (os == OsType.WINDOWS) return
        when (os) {
            OsType.LINUX -> prependEnv("LD_LIBRARY_PATH", libDir)
            OsType.MACOS -> {
                prependEnv("DYLD_LIBRARY_PATH", libDir)
                prependEnv("DYLD_FALLBACK_LIBRARY_PATH", libDir)
            }
            OsType.WINDOWS -> Unit
        }
    }

    /** 将 [value] 前置到名为 [key] 的环境变量（已存在则用路径分隔符拼接）。 */
    private fun ProcessBuilder.prependEnv(key: String, value: String) {
        val existing = environment()[key]
        environment()[key] = if (existing.isNullOrEmpty()) value else "$value${File.pathSeparator}$existing"
    }
}

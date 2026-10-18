package com.ominigifmaker.core.engine

import com.ominigifmaker.core.settings.SettingsStore
import com.ominigifmaker.core.utils.OsType
import com.ominigifmaker.core.utils.currentArch
import java.io.File

/**
 * 引擎二进制运行时提取器。
 *
 * 二进制在编译期随资源打包到 `binaries/<os>/`（或按架构细分的 `binaries/<os>/<arch>/`）
 * 目录，可执行文件与依赖库（Windows 的 DLL / macOS 的 dylib / Linux 的 so）平铺放置，
 * 由该目录下的 `manifest.txt` 清单列出需释放的文件。由于 ImageMagick / libvips 依赖
 * 同目录下的动态库，故采用「按清单整体释放整目录」而非「单可执行文件」的方式。
 *
 * 流程：检测宿主 OS（+ CPU 架构）→ 确定解压目标目录（默认 `java.io.tmpdir/gif_app_engines/`，
 * 可由 [SettingsStore.engineExtractionDir] 覆盖）→ 按 manifest 从 classpath 流式释放 → 非
 * Windows 平台赋予可执行权限 → 每次执行前校验存在性，缺失则重新释放。
 */
object EngineExtractor {

    private val os: OsType = OsType.detect()
    private val arch: String = currentArch()

    private val enginesRoot: File by lazy { File(SettingsStore.engineExtractionDir) }

    /**
     * classpath 上引擎 bundle 的资源目录名。
     *
     * 优先使用带架构的 `binaries/<os>/<arch>/`（macOS 的 arm64 / x86_64、Linux 的
     * aarch64 / x86_64 二进制互不通用）；若不存在架构子目录（如当前 Windows 仅提供
     * x86_64 的平铺目录），则回退到 `binaries/<os>/`。
     */
    private val bundleDir: String by lazy { resolveBundleDir() }

    /** 释放整个平台 bundle（主窗口加载前调用）。 */
    fun extractAllIfNeeded() {
        extractBundle()
    }

    /** 返回指定引擎在解压目录中的可执行文件路径。 */
    fun executableFile(engine: EngineType): File =
        File(enginesRoot, engine.executableName(os))

    /** 校验并（必要时）释放引擎：目标可执行文件缺失时重新释放整个 bundle。 */
    fun ensureExtracted(engine: EngineType) {
        if (!executableFile(engine).isFile) extractBundle()
    }

    private fun resolveBundleDir(): String {
        val archDir = "binaries/${os.dirName}/$arch"
        val flatDir = "binaries/${os.dirName}"
        val hasArchManifest =
            EngineExtractor::class.java.classLoader.getResource("$archDir/manifest.txt") != null
        return if (hasArchManifest) archDir else flatDir
    }

    private fun extractBundle() {
        val manifestPath = "$bundleDir/manifest.txt"
        val stream = EngineExtractor::class.java.classLoader.getResourceAsStream(manifestPath)
        if (stream == null) {
            System.err.println("Engine bundle manifest not found: $manifestPath")
            return
        }
        val files = stream.bufferedReader().use { reader ->
            reader.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith('#') }
        }
        files.forEach { fileName ->
            runCatching { extractFile(fileName) }
                .onFailure { System.err.println("Failed to extract $fileName: ${it.message}") }
        }
    }

    private fun extractFile(fileName: String) {
        val target = File(enginesRoot, fileName)
        if (target.isFile && target.length() > 0) return

        val resourcePath = "$bundleDir/$fileName"
        val stream = EngineExtractor::class.java.classLoader.getResourceAsStream(resourcePath)
        if (stream == null) {
            System.err.println("Engine file not found in resources: $resourcePath")
            return
        }
        target.parentFile?.mkdirs()
        stream.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        if (os != OsType.WINDOWS) {
            target.setExecutable(true)
        }
    }
}

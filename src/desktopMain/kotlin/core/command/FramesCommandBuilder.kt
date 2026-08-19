package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.FramesConfig
import com.ominigifmaker.model.FramesConverter
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import java.io.File

/**
 * Frames 模块命令生成：将帧图像序列合成 GIF。
 *
 * - ImageMagick / ImageMagick(-color 256)：`magick <frames...> -delay N -loop L out.gif`
 * - libvips：`vips arrayjoin --across 1` + `vips gifsave --page-height H`（由调用方两步执行）
 */
object FramesCommandBuilder {

    fun validate(config: FramesConfig, lang: Language): String? {
        val s = AppStrings(lang)
        if (config.framePaths.isEmpty()) return s.errFramesEmpty
        val delay = config.globalDelay.trim()
        if (delay.isNotEmpty() && delay.toIntOrNull() == null) return s.errFramesDelay
        val loop = config.loopCount.trim()
        if (loop.isNotEmpty() && loop.toIntOrNull() == null) return s.errFramesLoop
        return null
    }

    /** 将 GIF 拆分为 PNG 帧序列（`-coalesce` 解除优化并铺满每帧）。 */
    fun buildExplode(input: File, outputPattern: File): EngineCommand =
        EngineCommand(
            EngineType.IMAGEMAGICK,
            listOf(input.absolutePath, "-coalesce", outputPattern.absolutePath),
        )

    /** ImageMagick 单命令合成。 */
    fun buildImageMagick(frames: List<String>, output: File, config: FramesConfig): EngineCommand {
        val args = mutableListOf<String>()
        args += frames
        val delay = config.globalDelay.trim()
        if (delay.isNotEmpty()) args += listOf("-delay", delay)
        val loop = config.loopCount.trim()
        args += listOf("-loop", loop.ifEmpty { "0" })
        if (config.converter == FramesConverter.IMAGEMAGICK_COLOR256) args += listOf("-colors", "256")
        if (config.useGlobalColormap) args += listOf("-layers", "optimize")
        args += output.absolutePath
        return EngineCommand(EngineType.IMAGEMAGICK, args)
    }

    /** libvips 第一步：纵向拼接帧序列。libvips 在 Windows 会把路径中的反斜杠当转义，须统一为正斜杠。 */
    fun buildVipsJoin(frames: List<String>, joinedOutput: File): EngineCommand =
        EngineCommand(
            EngineType.LIBVIPS,
            listOf(
                "arrayjoin",
                frames.joinToString(" ") { it.replace('\\', '/') },
                joinedOutput.absolutePath.replace('\\', '/'),
                "--across",
                "1",
            ),
        )

    /** libvips 第二步：按页高保存为 GIF（路径同样归一化为正斜杠）。 */
    fun buildVipsSave(joinedInput: File, output: File, pageHeight: Int): EngineCommand =
        EngineCommand(
            EngineType.LIBVIPS,
            listOf(
                "gifsave",
                joinedInput.absolutePath.replace('\\', '/'),
                output.absolutePath.replace('\\', '/'),
                "--page-height",
                pageHeight.toString(),
            ),
        )
}

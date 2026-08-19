package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.ResizeConfig
import com.ominigifmaker.model.ResizeMethod
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import java.io.File

/**
 * 将 Resize 表单配置映射为对应引擎的 CLI 参数。
 *
 * - Gifsicle：`--resize` / `--resize-fit-width` / `--resize-fit-height` / `--scale`
 * - ImageMagick：`-resize`（单侧留空用 `Wx` / `xH` 等比）
 * - ImageMagick + coalesce：先 `-coalesce` 再 `-resize`，末尾 `-layers optimize`
 * - Change canvas size：`-extent` 扩展画布（居中、透明背景、不缩放）
 */
object ResizeCommandBuilder {

    /** 校验表单，返回错误信息或 null（通过）。 */
    fun validate(config: ResizeConfig, lang: Language): String? {
        val s = AppStrings(lang)
        val w = config.width.trim().toIntOrNull()
        val h = config.height.trim().toIntOrNull()
        val pct = config.percentage.trim().toIntOrNull()
        return when {
            config.method == ResizeMethod.CHANGE_CANVAS && (w == null || h == null) -> s.errResizeCanvas
            w == null && h == null && pct == null -> s.errResizeEmpty
            else -> null
        }
    }

    fun build(input: File, output: File, config: ResizeConfig): EngineCommand {
        val w = config.width.trim().toIntOrNull()
        val h = config.height.trim().toIntOrNull()
        val pct = config.percentage.trim().toIntOrNull()
        return when (config.method) {
            ResizeMethod.GIFSICLE -> gifsicle(w, h, pct, input, output)
            ResizeMethod.IMAGEMAGICK -> imageMagick(w, h, pct, coalesce = false, input, output)
            ResizeMethod.IMAGEMAGICK_COALESCE -> imageMagick(w, h, pct, coalesce = true, input, output)
            ResizeMethod.CHANGE_CANVAS -> changeCanvas(w, h, input, output)
        }
    }

    private fun gifsicle(w: Int?, h: Int?, pct: Int?, input: File, output: File): EngineCommand {
        val args = mutableListOf<String>()
        when {
            pct != null -> args += listOf("--scale", (pct / 100.0).toString())
            w != null && h != null -> args += listOf("--resize", "${w}x${h}")
            w != null -> args += listOf("--resize-fit-width", w.toString())
            h != null -> args += listOf("--resize-fit-height", h.toString())
        }
        args += listOf(input.absolutePath, "-o", output.absolutePath)
        return EngineCommand(EngineType.GIFSICLE, args)
    }

    private fun imageMagick(
        w: Int?,
        h: Int?,
        pct: Int?,
        coalesce: Boolean,
        input: File,
        output: File,
    ): EngineCommand {
        val args = mutableListOf<String>()
        args += input.absolutePath
        if (coalesce) args += "-coalesce"
        when {
            pct != null -> args += listOf("-resize", "$pct%")
            w != null && h != null -> args += listOf("-resize", "${w}x${h}")
            w != null -> args += listOf("-resize", "${w}x")
            h != null -> args += listOf("-resize", "x${h}")
        }
        if (coalesce) args += listOf("-layers", "optimize")
        args += output.absolutePath
        return EngineCommand(EngineType.IMAGEMAGICK, args)
    }

    private fun changeCanvas(w: Int?, h: Int?, input: File, output: File): EngineCommand {
        require(w != null && h != null) { "Change canvas size 需要同时填写 Width 和 Height。" }
        val args = mutableListOf<String>()
        args += input.absolutePath
        args += listOf(
            "-coalesce",
            "-background", "none",
            "-gravity", "center",
            "-extent", "${w}x${h}",
            "-layers", "optimize",
        )
        args += output.absolutePath
        return EngineCommand(EngineType.IMAGEMAGICK, args)
    }
}

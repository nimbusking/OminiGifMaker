package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.RotateConfig
import com.ominigifmaker.model.RotateMode
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import java.io.File

/**
 * 将 Rotate 表单配置映射为对应引擎的 CLI 参数。
 *
 * - 90°/180°/翻转 → Gifsicle（`--rotate-90` / `--rotate-270` / `--rotate-180` / `--flip-*`）
 * - 自定义角度 → ImageMagick（`-coalesce -background none -rotate <deg> +repage -layers optimize`）
 */
object RotateCommandBuilder {

    /** 校验表单，返回错误信息或 null（通过）。 */
    fun validate(config: RotateConfig, lang: Language): String? {
        val s = AppStrings(lang)
        if (config.rotateMode == RotateMode.CUSTOM) {
            return if (config.customDegrees.trim().toIntOrNull() == null) s.errRotateAngle else null
        }
        val hasFlip = config.flipVertical || config.flipHorizontal
        val hasRotate = config.rotateMode != RotateMode.NONE
        return if (!hasFlip && !hasRotate) s.errRotateEmpty else null
    }

    fun build(input: File, output: File, config: RotateConfig): EngineCommand =
        if (config.rotateMode == RotateMode.CUSTOM) imageMagick(input, output, config)
        else gifsicle(input, output, config)

    private fun gifsicle(input: File, output: File, config: RotateConfig): EngineCommand {
        val args = mutableListOf<String>()
        when {
            config.flipHorizontal && config.flipVertical -> args += "--rotate-180"
            config.flipHorizontal -> args += "--flip-horizontal"
            config.flipVertical -> args += "--flip-vertical"
        }
        when (config.rotateMode) {
            RotateMode.CLOCKWISE_90 -> args += "--rotate-90"
            RotateMode.COUNTERCLOCKWISE_90 -> args += "--rotate-270"
            RotateMode.ROTATE_180 -> args += "--rotate-180"
            RotateMode.NONE, RotateMode.CUSTOM -> Unit
        }
        args += listOf(input.absolutePath, "-o", output.absolutePath)
        return EngineCommand(EngineType.GIFSICLE, args)
    }

    private fun imageMagick(input: File, output: File, config: RotateConfig): EngineCommand {
        val deg = config.customDegrees.trim().toInt()
        val args = mutableListOf<String>()
        args += input.absolutePath
        args += "-coalesce"
        if (config.flipVertical) args += "-flip"
        if (config.flipHorizontal) args += "-flop"
        args += listOf(
            "-background", "none",
            "-rotate", deg.toString(),
            "+repage",
            "-layers", "optimize",
        )
        args += output.absolutePath
        return EngineCommand(EngineType.IMAGEMAGICK, args)
    }
}

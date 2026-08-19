package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.CropConfig
import com.ominigifmaker.model.CropMethod
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import java.io.File

/**
 * 将 Crop 表单配置映射为对应引擎的 CLI 参数。
 *
 * - Gifsicle：`--crop L,T+WxH`（裁剪）/ `--crop-transparency`（自动裁剪）
 * - ImageMagick：`-crop WxH+L+T +repage`（裁剪）/ `-fuzz 0% -trim +repage`（自动裁剪）
 * - ImageMagick + coalesce：先 `-coalesce`，末尾 `-layers optimize`
 */
object CropCommandBuilder {

    /** 校验表单，返回错误信息或 null（通过）。 */
    fun validate(config: CropConfig, lang: Language): String? {
        if (config.autocrop) return null
        val s = AppStrings(lang)
        val l = config.left.trim().toIntOrNull()
        val t = config.top.trim().toIntOrNull()
        val w = config.width.trim().toIntOrNull()
        val h = config.height.trim().toIntOrNull()
        return when {
            l == null || t == null || w == null || h == null -> s.errCropIncomplete
            w <= 0 || h <= 0 -> s.errCropPositive
            else -> null
        }
    }

    fun build(input: File, output: File, config: CropConfig): EngineCommand =
        if (config.autocrop) autocrop(input, output, config.method)
        else crop(input, output, config)

    private fun crop(input: File, output: File, config: CropConfig): EngineCommand {
        val l = config.left.trim().toInt()
        val t = config.top.trim().toInt()
        val w = config.width.trim().toInt()
        val h = config.height.trim().toInt()
        return when (config.method) {
            CropMethod.GIFSICLE -> EngineCommand(
                EngineType.GIFSICLE,
                listOf("--crop", "$l,$t+${w}x${h}", input.absolutePath, "-o", output.absolutePath),
            )

            CropMethod.IMAGEMAGICK -> magickCrop(l, t, w, h, coalesce = false, input, output)
            CropMethod.IMAGEMAGICK_COALESCE -> magickCrop(l, t, w, h, coalesce = true, input, output)
        }
    }

    private fun magickCrop(
        l: Int,
        t: Int,
        w: Int,
        h: Int,
        coalesce: Boolean,
        input: File,
        output: File,
    ): EngineCommand {
        val args = mutableListOf<String>()
        args += input.absolutePath
        if (coalesce) args += "-coalesce"
        args += listOf("-crop", "${w}x${h}+${l}+${t}", "+repage")
        if (coalesce) args += listOf("-layers", "optimize")
        args += output.absolutePath
        return EngineCommand(EngineType.IMAGEMAGICK, args)
    }

    private fun autocrop(input: File, output: File, method: CropMethod): EngineCommand =
        when (method) {
            CropMethod.GIFSICLE -> EngineCommand(
                EngineType.GIFSICLE,
                listOf("--crop-transparency", input.absolutePath, "-o", output.absolutePath),
            )

            CropMethod.IMAGEMAGICK,
            CropMethod.IMAGEMAGICK_COALESCE -> {
                val args = mutableListOf<String>()
                args += input.absolutePath
                if (method == CropMethod.IMAGEMAGICK_COALESCE) args += "-coalesce"
                args += listOf("-fuzz", "0%", "-trim", "+repage")
                args += listOf("-layers", "optimize")
                args += output.absolutePath
                EngineCommand(EngineType.IMAGEMAGICK, args)
            }
        }
}

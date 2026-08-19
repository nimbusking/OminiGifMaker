package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.OptimizeConfig
import com.ominigifmaker.model.OptimizeMethod
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import java.io.File

/**
 * Optimize 模块命令生成（Gifsicle 优化）。
 *
 * 「Reencode with gifski」为多步（拆帧 → gifski），由调用方单独处理。
 */
object OptimizeCommandBuilder {

    fun validate(config: OptimizeConfig, lang: Language): String? {
        val s = AppStrings(lang)
        val lossy = config.lossyLevel.trim().toIntOrNull()
        if (lossy == null || lossy !in 1..200) return s.errOptimizeLossy
        val colors = config.colors.trim().toIntOrNull()
        if (colors == null || colors !in 2..256) return s.errOptimizeColors
        return null
    }

    /** 生成 Gifsicle 优化命令（不含 gifski 方法）。 */
    fun build(input: File, output: File, config: OptimizeConfig): EngineCommand {
        val args = mutableListOf<String>()
        val lossy = config.lossyLevel.trim()
        val colors = config.colors.trim()

        when (config.method) {
            OptimizeMethod.LOSSY -> args += "--lossy=$lossy"
            OptimizeMethod.COMBINED -> {
                args += "-O3"
                args += "--lossy=$lossy"
            }

            OptimizeMethod.COLOR_REDUCTION -> args += "--colors=$colors"
            OptimizeMethod.COLOR_REDUCTION_DITHER -> {
                args += "--colors=$colors"
                args += "--dither"
            }

            OptimizeMethod.SINGLE_COLOR_TABLE -> args += "-O2"
            OptimizeMethod.REMOVE_DUPLICATES -> args += "-O3"
            OptimizeMethod.OPTIMIZE_TRANSPARENCY -> args += "-O2"
            OptimizeMethod.COALESCE -> args += "--unoptimize"
            OptimizeMethod.REENCODE_GIFSKI -> Unit // 由调用方处理
        }

        if (config.eliminateLocalTables) args += "-O3"

        args += input.absolutePath
        args += listOf("-o", output.absolutePath)
        return EngineCommand(EngineType.GIFSICLE, args)
    }
}

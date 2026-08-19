package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.ReverseConfig
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import java.io.File

/**
 * Reverse 模块命令生成。
 *
 * - 倒放 / 回旋镖 / 循环 / 翻转 → Gifsicle（帧选择 `#-1-0` 倒放，`#0-` + `#-2-1` 回旋镖）
 * - 计时器叠加 → FFmpeg `drawtext`（跨模块共享能力，由调用方链式执行）
 */
object ReverseCommandBuilder {

    fun validate(config: ReverseConfig, lang: Language): String? {
        val s = AppStrings(lang)
        val loop = config.loopCount.trim()
        if (loop.isNotEmpty() && loop.toIntOrNull() == null) return s.errReverseLoop
        val hasOp = config.reverse || config.boomerang || config.addTimer ||
            config.flipVertical || config.flipHorizontal || loop.isNotEmpty()
        return if (!hasOp) s.errReverseEmpty else null
    }

    /** Gifsicle 部分：倒放/回旋镖/循环/翻转。 */
    fun buildGifsicle(input: File, output: File, config: ReverseConfig): EngineCommand {
        val args = mutableListOf<String>()
        when {
            config.flipHorizontal && config.flipVertical -> args += "--rotate-180"
            config.flipHorizontal -> args += "--flip-horizontal"
            config.flipVertical -> args += "--flip-vertical"
        }

        when {
            config.boomerang -> {
                args += input.absolutePath
                args += "#0-"
                args += input.absolutePath
                args += "#-2-1"
            }

            config.reverse -> {
                args += input.absolutePath
                args += "#-1-0"
            }

            else -> args += input.absolutePath
        }

        val loop = config.loopCount.trim()
        if (loop.isNotEmpty()) args += "--loopcount=$loop"
        args += listOf("-o", output.absolutePath)
        return EngineCommand(EngineType.GIFSICLE, args)
    }

    /** FFmpeg 计时器叠加：在画面上显示已播放时长（HH:MM:SS）。 */
    fun buildTimerOverlay(input: File, output: File): EngineCommand {
        val vf = "drawtext=text='%{pts\\:hms}':fontcolor=white:fontsize=24:box=1:boxcolor=black@0.5:boxborderw=6"
        return EngineCommand(
            EngineType.FFMPEG,
            listOf("-y", "-i", input.absolutePath, "-vf", vf, output.absolutePath),
        )
    }
}

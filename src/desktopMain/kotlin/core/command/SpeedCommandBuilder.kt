package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.SpeedConfig
import com.ominigifmaker.model.SpeedMode
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import java.io.File
import kotlin.math.roundToInt

/**
 * Speed 模块命令生成。
 *
 * - `% of current speed` → FFmpeg `setpts` 缩放时间戳（配合 fps 保持/复制帧）
 * - `hundredths of second between frames` → Gifsicle `--delay` 设置绝对延迟
 */
object SpeedCommandBuilder {

    fun validate(config: SpeedConfig, lang: Language): String? {
        val v = config.value.trim().toIntOrNull()
        return if (v == null || v <= 0) AppStrings(lang).errSpeedValue else null
    }

    fun build(input: File, output: File, config: SpeedConfig, sourceFps: Double): EngineCommand {
        val v = config.value.trim().toInt()
        return if (config.mode == SpeedMode.HUNDREDTHS) {
            EngineCommand(
                EngineType.GIFSICLE,
                listOf("--delay", v.toString(), input.absolutePath, "-o", output.absolutePath),
            )
        } else {
            val factor = v / 100.0
            // 加速时按比例提高输出帧率保持帧数；减速时维持原帧率以复制帧。
            val fps = (sourceFps * factor.coerceAtLeast(1.0)).roundToInt().coerceAtLeast(1)
            val vf = "setpts=PTS/$factor,fps=$fps"
            EngineCommand(
                EngineType.FFMPEG,
                listOf("-y", "-i", input.absolutePath, "-vf", vf, output.absolutePath),
            )
        }
    }
}

package com.ominigifmaker.model

/** 速度调整模式。 */
enum class SpeedMode(val label: String) {
    PERCENT("% of current speed"),
    HUNDREDTHS("hundredths of second between frames"),
}

/** Speed 模块表单配置。 */
data class SpeedConfig(
    val mode: SpeedMode = SpeedMode.PERCENT,
    /** 百分比数值（如 200）或帧间延迟（1/100 秒）。 */
    val value: String = "",
)

package com.ominigifmaker.model

/** Reverse 模块表单配置。 */
data class ReverseConfig(
    val reverse: Boolean = false,
    val boomerang: Boolean = false,
    /** 循环次数（空串表示无限循环）。 */
    val loopCount: String = "",
    val addTimer: Boolean = false,
    val flipVertical: Boolean = false,
    val flipHorizontal: Boolean = false,
)

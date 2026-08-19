package com.ominigifmaker.model

/**
 * 旋转模式。90°/180°/翻转走 Gifsicle（原生、无损），
 * [CUSTOM] 自定义角度走 ImageMagick（需先 coalesce）。
 */
enum class RotateMode(val label: String) {
    NONE("None"),
    CLOCKWISE_90("90° clockwise"),
    COUNTERCLOCKWISE_90("90° counter-clockwise"),
    ROTATE_180("180°"),
    CUSTOM("Custom angle"),
}

/** Rotate 模块表单配置。 */
data class RotateConfig(
    val flipVertical: Boolean = false,
    val flipHorizontal: Boolean = false,
    val rotateMode: RotateMode = RotateMode.NONE,
    /** 自定义角度（度，整数；仅当 [rotateMode] 为 [RotateMode.CUSTOM] 时生效）。 */
    val customDegrees: String = "",
)

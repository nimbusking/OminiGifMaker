package com.ominigifmaker.model

/** Resize 模块的处理方法。 */
enum class ResizeMethod(val label: String) {
    GIFSICLE("Gifsicle (fastest, smallest file size)"),
    IMAGEMAGICK("ImageMagick"),
    IMAGEMAGICK_COALESCE("ImageMagick + coalesce (undo optimizations)"),
    CHANGE_CANVAS("Change canvas size (add padding without scaling)"),
}

/**
 * Resize 模块表单配置。
 *
 * [width]/[height] 为像素值（空串表示留空，单侧留空按原图比例等比缩放）；
 * [percentage] 为百分比缩放（与像素值互斥，填写时优先于像素值）。
 */
data class ResizeConfig(
    val width: String = "",
    val height: String = "",
    val percentage: String = "",
    val method: ResizeMethod = ResizeMethod.GIFSICLE,
)

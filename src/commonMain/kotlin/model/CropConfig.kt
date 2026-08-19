package com.ominigifmaker.model

/** Crop 模块的处理引擎。 */
enum class CropMethod(val label: String) {
    GIFSICLE("Gifsicle"),
    IMAGEMAGICK("ImageMagick"),
    IMAGEMAGICK_COALESCE("ImageMagick + coalesce"),
}

/** Crop 预览画布的背景。 */
enum class CropBackground(val label: String) {
    CHECKERED("Checkered pattern"),
    WHITE("White"),
    BLACK("Black"),
}

/** 裁剪锁定长宽比。[ratio] 为 null 表示自由裁剪。 */
enum class CropAspectRatio(val label: String, val ratio: Double?) {
    FREE("Free", null),
    SQUARE("Square (1:1)", 1.0),
    FOUR_THREE("4:3", 4.0 / 3.0),
    SIXTEEN_NINE("16:9", 16.0 / 9.0),
    THREE_FOUR("3:4", 3.0 / 4.0),
    NINE_SIXTEEN("9:16", 9.0 / 16.0),
}

/**
 * Crop 模块表单配置。
 *
 * [left]/[top]/[width]/[height] 为像素值（空串表示未设置）；[autocrop] 勾选时忽略裁剪矩形，
 * 自动去除透明边缘。
 */
data class CropConfig(
    val left: String = "",
    val top: String = "",
    val width: String = "",
    val height: String = "",
    val method: CropMethod = CropMethod.GIFSICLE,
    val background: CropBackground = CropBackground.CHECKERED,
    val lockAspect: CropAspectRatio = CropAspectRatio.FREE,
    val autocrop: Boolean = false,
    val dontScaleLarge: Boolean = false,
)

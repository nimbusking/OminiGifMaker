package com.ominigifmaker.model

/** Resize 模块的处理方法。 */
enum class ResizeMethod(val label: String) {
    GIFSICLE("Gifsicle (fastest, smallest file size)"),
    IMAGEMAGICK("ImageMagick"),
    IMAGEMAGICK_COALESCE("ImageMagick + coalesce (undo optimizations)"),
    CHANGE_CANVAS("Change canvas size (add padding without scaling)");

    /**
     * 该引擎支持的宽高比策略。
     * - Gifsicle 仅支持 Stretch（其余策略需裁边/透明填充，gifsicle 无此能力）。
     * - ImageMagick（含 coalesce）支持全部。
     * - Change canvas size 与宽高比策略无关，返回空。
     */
    val supportedAspectModes: List<ResizeAspectMode>
        get() = when (this) {
            GIFSICLE -> listOf(ResizeAspectMode.STRETCH)
            IMAGEMAGICK, IMAGEMAGICK_COALESCE -> ResizeAspectMode.entries.toList()
            CHANGE_CANVAS -> emptyList()
        }
}

/** Resize 模块的宽高比不匹配处理策略。 */
enum class ResizeAspectMode(val label: String) {
    CENTER_CROP("Center and crop to fit"),
    STRETCH("Stretch to fit"),
    FORCE_ORIGINAL("Force original aspect ratio"),
    TRANSPARENT_PAD("Add transparent padding"),
}

/**
 * Resize 模块表单配置。
 *
 * [width]/[height] 为像素值（空串表示留空，单侧留空按原图比例等比缩放）；
 * [percentage] 为百分比缩放（与像素值互斥，填写时优先于像素值）；
 * [aspectMode] 为宽高比不匹配时的处理策略（仅当宽高都填写时生效）。
 */
data class ResizeConfig(
    val width: String = "",
    val height: String = "",
    val percentage: String = "",
    val method: ResizeMethod = ResizeMethod.GIFSICLE,
    val aspectMode: ResizeAspectMode = ResizeAspectMode.STRETCH,
)

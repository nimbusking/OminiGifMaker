package com.ominigifmaker.model

/** 帧序列转 GIF 的转换引擎。 */
enum class FramesConverter(val label: String) {
    LIBVIPS("libvips"),
    IMAGEMAGICK("ImageMagick"),
    IMAGEMAGICK_COLOR256("ImageMagick (-color 256)"),
}

/**
 * 帧网格中的单帧条目。
 *
 * - [path]：帧图像文件路径（拆帧后的 PNG）。
 * - [skipped]：是否被标记为跳过，合成 GIF 时被剔除。
 */
data class FrameEntry(
    val path: String,
    val skipped: Boolean = false,
)

/** Frames 模块表单配置。 */
data class FramesConfig(
    /** 帧图像文件路径序列（按顺序）。 */
    val framePaths: List<String> = emptyList(),
    /** 全局统一延迟（1/100 秒，空串表示使用各帧默认）。 */
    val globalDelay: String = "",
    /** 循环次数（空串表示无限循环）。 */
    val loopCount: String = "",
    val useGlobalColormap: Boolean = false,
    val converter: FramesConverter = FramesConverter.IMAGEMAGICK,
)

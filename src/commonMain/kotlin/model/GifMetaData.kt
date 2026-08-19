package com.ominigifmaker.model

/** GIF 文件基础元数据，统一由 ffprobe 读取填充。 */
data class GifMetaData(
    /** 文件体积（字节）。 */
    val size: Long,
    /** 宽度（像素）。 */
    val width: Int,
    /** 高度（像素）。 */
    val height: Int,
    /** 总帧数。 */
    val frameCount: Int,
    /** 格式名（如 gif）。 */
    val format: String,
    /** 总时长（秒）。 */
    val duration: Double,
)

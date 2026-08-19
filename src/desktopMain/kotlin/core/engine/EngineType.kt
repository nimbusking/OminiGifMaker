package com.ominigifmaker.core.engine

import com.ominigifmaker.core.utils.OsType

/**
 * 内置引擎枚举与可执行文件映射。
 *
 * 二进制在编译期随资源打包到 `binaries/<os>/` 目录（可执行文件与依赖 DLL 平铺放置），
 * 运行时由 [EngineExtractor] 按 manifest 解压到临时目录后调用。
 */
enum class EngineType(val baseName: String) {
    FFMPEG("ffmpeg"),
    FFPROBE("ffprobe"),
    GIFSICLE("gifsicle"),
    /** ImageMagick IM7 的 `magick` 命令。 */
    IMAGEMAGICK("magick"),
    GIFSKI("gifski"),
    LIBVIPS("vips"),
    ;

    /** 目标平台上的可执行文件名（Windows 带 .exe 后缀）。 */
    fun executableName(os: OsType): String = baseName + os.executableExtension
}

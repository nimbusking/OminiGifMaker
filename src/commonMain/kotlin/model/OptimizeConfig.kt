package com.ominigifmaker.model

/** 优化方法。 */
enum class OptimizeMethod(val label: String) {
    LOSSY("Lossy GIF"),
    REENCODE_GIFSKI("Reencode with gifski"),
    COMBINED("Combined: remove duplicates + transparency + lossy"),
    COLOR_REDUCTION("Color Reduction"),
    COLOR_REDUCTION_DITHER("Color Reduction + dither"),
    SINGLE_COLOR_TABLE("Use single color table for all frames"),
    REMOVE_DUPLICATES("Remove duplicate frames"),
    OPTIMIZE_TRANSPARENCY("Optimize Transparency"),
    COALESCE("Coalesce (unoptimize)"),
}

/** Optimize 模块表单配置。 */
data class OptimizeConfig(
    val method: OptimizeMethod = OptimizeMethod.LOSSY,
    /** 有损压缩级别（1-200）。 */
    val lossyLevel: String = "80",
    /** 色彩数量（用于色彩减少类方法）。 */
    val colors: String = "256",
    val eliminateLocalTables: Boolean = false,
)

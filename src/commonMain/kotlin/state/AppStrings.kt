package com.ominigifmaker.state

import com.ominigifmaker.model.CropAspectRatio
import com.ominigifmaker.model.CropBackground
import com.ominigifmaker.model.CropMethod
import com.ominigifmaker.model.FramesConverter
import com.ominigifmaker.model.OptimizeMethod
import com.ominigifmaker.model.ResizeMethod
import com.ominigifmaker.model.RotateMode
import com.ominigifmaker.model.SpeedMode

/**
 * 集中式文案，根据 [lang] 返回中文或英文。
 *
 * UI 层通过 [com.ominigifmaker.ui.LocalAppStrings] 获取当前语言对应的实例，
 * 所有用户可见文案统一从这里取，便于「中英文切换」。
 */
class AppStrings(val lang: Language) {

    private val zh: Boolean get() = lang == Language.ZH

    private fun t(zhText: String, enText: String): String = if (zh) zhText else enText

    // ---- 导航 ----
    val settings: String get() = t("设置", "Settings")
    val about: String get() = t("关于", "About")

    fun tabLabel(tab: AppTab): String = if (zh) {
        when (tab) {
            AppTab.Resize -> "调整大小"
            AppTab.Crop -> "裁剪"
            AppTab.Rotate -> "旋转"
            AppTab.Reverse -> "倒放"
            AppTab.Speed -> "速度"
            AppTab.Optimize -> "优化"
            AppTab.Frames -> "帧"
            AppTab.Effects -> "特效"
            AppTab.AddText -> "添加文本"
            AppTab.Censor -> "打码"
            AppTab.AddImage -> "添加图像"
            AppTab.Cut -> "剪辑"
            AppTab.Split -> "拆分"
        }
    } else {
        tab.label
    }

    val phaseTwoPlaceholder: String get() = t("二期预留功能（占位）。", "Reserved feature (phase 2 placeholder).")

    // ---- 文件元数据区 ----
    val sourceFile: String get() = t("源文件", "Source file")
    val chooseGif: String get() = t("选择 GIF…", "Choose GIF…")
    val clear: String get() = t("清除", "Clear")
    val noFileSelected: String get() = t("尚未选择文件。", "No file selected.")
    val readingMetadata: String get() = t("读取元数据中…", "Reading metadata…")
    val dimensions: String get() = t("尺寸", "Dimensions")
    val frames: String get() = t("帧数", "Frames")
    val format: String get() = t("格式", "Format")
    val duration: String get() = t("时长", "Duration")
    val size: String get() = t("体积", "Size")
    val path: String get() = t("路径", "Path")

    // ---- 结果区 ----
    val result: String get() = t("结果", "Result")
    val resultPlaceholder: String get() = t("处理结果将显示在这里。", "The result will appear here.")
    val processing: String get() = t("处理中…", "Processing…")
    val outputFile: String get() = t("输出文件", "Output file")

    // ---- 通用 ----
    val rememberSettings: String get() = t("Remember settings", "Remember settings")

    // ---- Resize ----
    val resizeTitle: String get() = t("调整大小", "Resize")
    val width: String get() = t("Width (px)", "Width (px)")
    val height: String get() = t("Height (px)", "Height (px)")
    val resizeHint: String get() = t("单侧留空按原图比例等比缩放；双侧填写则拉伸变形。", "Leave one side blank to scale proportionally; fill both to stretch.")
    val percentage: String get() = t("Percentage (%)", "Percentage (%)")
    val resizeMethod: String get() = t("Resize method", "Resize method")
    val resizeButton: String get() = t("Resize image!", "Resize image!")

    fun resizeMethodLabel(m: ResizeMethod): String = if (zh) {
        when (m) {
            ResizeMethod.GIFSICLE -> "Gifsicle（最快、文件最小）"
            ResizeMethod.IMAGEMAGICK -> "ImageMagick"
            ResizeMethod.IMAGEMAGICK_COALESCE -> "ImageMagick + coalesce（解除优化）"
            ResizeMethod.CHANGE_CANVAS -> "修改画布尺寸（加边距不缩放）"
        }
    } else {
        m.label
    }

    // ---- Crop ----
    val cropTitle: String get() = t("裁剪", "Crop")
    val left: String get() = t("Left", "Left")
    val top: String get() = t("Top", "Top")
    val lockAspect: String get() = t("Lock aspect ratio", "Lock aspect ratio")
    val cropWith: String get() = t("Crop with", "Crop with")
    val autocrop: String get() = t("Autocrop（去除透明边缘）", "Autocrop (trim transparent pixels around the image)")
    val background: String get() = t("Background", "Background")
    val dontScaleLarge: String get() = t("Don't scale large images", "Don't scale large images")
    val cropButton: String get() = t("Crop image!", "Crop image!")
    val cropPreviewPlaceholder: String get() = t("选择源 GIF 后可预览裁剪区域", "Choose a source GIF to preview the crop area")

    fun cropMethodLabel(m: CropMethod): String = if (zh) {
        when (m) {
            CropMethod.GIFSICLE -> "Gifsicle"
            CropMethod.IMAGEMAGICK -> "ImageMagick"
            CropMethod.IMAGEMAGICK_COALESCE -> "ImageMagick + coalesce"
        }
    } else {
        m.label
    }

    fun cropBackgroundLabel(b: CropBackground): String = if (zh) {
        when (b) {
            CropBackground.CHECKERED -> "棋盘格"
            CropBackground.WHITE -> "白色"
            CropBackground.BLACK -> "黑色"
        }
    } else {
        b.label
    }

    fun cropAspectLabel(a: CropAspectRatio): String = if (zh) {
        when (a) {
            CropAspectRatio.FREE -> "自由"
            CropAspectRatio.SQUARE -> "正方形 (1:1)"
            CropAspectRatio.FOUR_THREE -> "4:3"
            CropAspectRatio.SIXTEEN_NINE -> "16:9"
            CropAspectRatio.THREE_FOUR -> "3:4"
            CropAspectRatio.NINE_SIXTEEN -> "9:16"
        }
    } else {
        a.label
    }

    // ---- Rotate ----
    val rotateTitle: String get() = t("旋转与翻转", "Rotate & Flip")
    val flip: String get() = t("Flip", "Flip")
    val flipVertical: String get() = t("Flip vertical", "Flip vertical")
    val flipHorizontal: String get() = t("Flip horizontal", "Flip horizontal")
    val rotate: String get() = t("Rotate", "Rotate")
    val degrees: String get() = t("Degrees", "Degrees")
    val applyRotation: String get() = t("Apply rotation!", "Apply rotation!")

    fun rotateModeLabel(m: RotateMode): String = if (zh) {
        when (m) {
            RotateMode.NONE -> "无"
            RotateMode.CLOCKWISE_90 -> "顺时针 90°"
            RotateMode.COUNTERCLOCKWISE_90 -> "逆时针 90°"
            RotateMode.ROTATE_180 -> "180°"
            RotateMode.CUSTOM -> "自定义角度"
        }
    } else {
        m.label
    }

    // ---- Reverse ----
    val reverseTitle: String get() = t("倒放与播放控制", "Reverse & Playback")
    val reverse: String get() = t("Reverse（倒放）", "Reverse")
    val boomerang: String get() = t("Boomerang（正放后倒放）", "Boomerang (run forward then back)")
    val addTimer: String get() = t("添加计时器", "Add second counter")
    val loopCount: String get() = t("Loop count（留空为无限循环）", "Loop count (empty = infinite)")
    val submit: String get() = t("Submit!", "Submit!")

    // ---- Speed ----
    val speedTitle: String get() = t("速度调整", "Speed")
    val speedMode: String get() = t("调节模式", "Mode")
    val targetSpeed: String get() = t("目标速度（%，如 200 = 2 倍速）", "Target speed (%, e.g. 200 = 2×)")
    val frameDelay: String get() = t("帧间延迟（1/100 秒）", "Delay between frames (1/100 s)")
    val changeSpeed: String get() = t("Change speed!", "Change speed!")

    fun speedModeLabel(m: SpeedMode): String = if (zh) {
        when (m) {
            SpeedMode.PERCENT -> "当前速度百分比"
            SpeedMode.HUNDREDTHS -> "帧间延迟（1/100 秒）"
        }
    } else {
        m.label
    }

    // ---- Optimize ----
    val optimizeTitle: String get() = t("优化与压缩", "Optimize")
    val optimizationMethod: String get() = t("Optimization method", "Optimization method")
    val compressionLevel: String get() = t("Compression level（1-200）", "Compression level (1-200)")
    val colors: String get() = t("Colors（2-256）", "Colors (2-256)")
    val eliminateLocalTables: String get() = t("Eliminate local color tables", "Eliminate local color tables")
    val optimizeButton: String get() = t("Optimize GIF!", "Optimize GIF!")

    fun optimizeMethodLabel(m: OptimizeMethod): String = if (zh) {
        when (m) {
            OptimizeMethod.LOSSY -> "Lossy GIF（有损压缩）"
            OptimizeMethod.REENCODE_GIFSKI -> "Reencode with gifski"
            OptimizeMethod.COMBINED -> "组合：去重复 + 透明度 + 有损"
            OptimizeMethod.COLOR_REDUCTION -> "减少颜色"
            OptimizeMethod.COLOR_REDUCTION_DITHER -> "减少颜色 + 抖动"
            OptimizeMethod.SINGLE_COLOR_TABLE -> "所有帧使用单一颜色表"
            OptimizeMethod.REMOVE_DUPLICATES -> "删除重复帧"
            OptimizeMethod.OPTIMIZE_TRANSPARENCY -> "优化透明度"
            OptimizeMethod.COALESCE -> "Coalesce（解除优化）"
        }
    } else {
        m.label
    }

    // ---- Frames ----
    val framesTitle: String get() = t("帧管理与合成", "Frames")
    val uploadFrames: String get() = t("+ 上传更多帧", "+ Upload more frames")
    val noFrames: String get() = t("尚未添加帧图像。点击上方按钮选择图像文件（PNG/JPG/GIF/WebP 等）。", "No frames yet. Click the button above to add image files (PNG/JPG/GIF/WebP, etc.).")
    val framesCount: String get() = t("已添加", "Added")
    val remove: String get() = t("移除", "Remove")
    val clearFrames: String get() = t("清空", "Clear all")
    val delayTime: String get() = t("Delay time（全局延迟，1/100 秒，留空用各帧默认）", "Delay time (global, 1/100 s, empty = per-frame default)")
    val useGlobalColormap: String get() = t("Use global colormap", "Use global colormap")
    val converter: String get() = t("Converter", "Converter")
    val makeGif: String get() = t("Make a GIF!", "Make a GIF!")

    fun framesConverterLabel(c: FramesConverter): String = if (zh) {
        when (c) {
            FramesConverter.LIBVIPS -> "libvips"
            FramesConverter.IMAGEMAGICK -> "ImageMagick"
            FramesConverter.IMAGEMAGICK_COLOR256 -> "ImageMagick (-color 256)"
        }
    } else {
        c.label
    }

    // ---- 设置页 ----
    val settingsTitle: String get() = t("设置", "Settings")
    val language: String get() = t("语言", "Language")
    val engine: String get() = t("引擎", "Engine")
    val engineDir: String get() = t("引擎解压目录", "Engine extraction directory")
    val engineDirHint: String get() = t("引擎二进制在首次运行时解压到该目录（ImageMagick / libvips 依赖同目录下的 DLL）。", "Engine binaries are extracted here on first run (ImageMagick / libvips need DLLs in the same directory).")
    val save: String get() = t("保存", "Save")
    val data: String get() = t("数据", "Data")
    val rememberHint: String get() = t("勾选「Remember settings」后，各模块表单参数保存在本机 Preferences 中。", "When \"Remember settings\" is checked, form parameters are saved to Preferences on this machine.")
    val clearAllSettings: String get() = t("清除所有已保存设置", "Clear all saved settings")
    val cleared: String get() = t("已清除所有设置。", "All settings cleared.")

    // ---- 关于页 ----
    val aboutTitle: String get() = t("关于", "About")
    val appName: String get() = "OminiGifMaker"
    val version: String get() = t("版本 1.0.0", "Version 1.0.0")
    val aboutDescription: String get() = t(
        "跨平台桌面端 GIF 处理工具，本地完成所有图像处理。内置 FFmpeg、Gifsicle、ImageMagick、gifski、libvips 五个引擎，以独立子进程调用，开箱即用。",
        "A cross-platform desktop GIF tool that does all processing locally. Bundles FFmpeg, Gifsicle, ImageMagick, gifski and libvips, invoked as separate subprocesses, ready to use.",
    )
    val license: String get() = t("许可证：GPL-3.0", "License: GPL-3.0")
    val thirdParty: String get() = t(
        "第三方组件及其许可证详见仓库根目录 THIRD_PARTY_NOTICES.md 与 third_party/ 目录。",
        "Third-party components and their licenses are listed in THIRD_PARTY_NOTICES.md and the third_party/ directory.",
    )

    // ---- 校验 / 错误 ----
    val selectSourceFirst: String get() = t("请先选择源 GIF 文件。", "Please select a source GIF first.")
    val processFailed: (Int) -> String = { code -> t("处理失败（退出码 $code）。", "Processing failed (exit code $code).") }
    val executionFailed: String get() = t("执行失败。", "Execution failed.")
    val readMetadataFailed: String get() = t("读取元数据失败", "Failed to read metadata")

    val errResizeCanvas: String get() = t("Change canvas size 需要同时填写 Width 和 Height。", "Change canvas size requires both Width and Height.")
    val errResizeEmpty: String get() = t("请至少填写 Width / Height / Percentage 之一。", "Fill in at least one of Width / Height / Percentage.")
    val errCropIncomplete: String get() = t("请完整填写 Left / Top / Width / Height，或勾选 Autocrop。", "Fill in Left / Top / Width / Height, or check Autocrop.")
    val errCropPositive: String get() = t("Width / Height 必须为正数。", "Width / Height must be positive.")
    val errRotateAngle: String get() = t("请输入有效的自定义角度（整数）。", "Enter a valid custom angle (integer).")
    val errRotateEmpty: String get() = t("请选择旋转或翻转操作。", "Choose a rotation or flip operation.")
    val errReverseLoop: String get() = t("循环次数必须为正整数（留空为无限循环）。", "Loop count must be a positive integer (empty = infinite).")
    val errReverseEmpty: String get() = t("请至少选择一项操作。", "Choose at least one operation.")
    val errSpeedValue: String get() = t("请输入有效的正数数值。", "Enter a valid positive number.")
    val errOptimizeLossy: String get() = t("有损压缩级别需在 1-200 之间。", "Lossy level must be 1-200.")
    val errOptimizeColors: String get() = t("色彩数量需在 2-256 之间。", "Color count must be 2-256.")
    val errFramesEmpty: String get() = t("请先添加至少一帧图像。", "Add at least one frame image.")
    val errFramesDelay: String get() = t("全局延迟必须为正整数（1/100 秒）。", "Global delay must be a positive integer (1/100 s).")
    val errFramesLoop: String get() = t("循环次数必须为正整数（留空为无限）。", "Loop count must be a positive integer (empty = infinite).")

    val hintUseCoalesce: String get() = t("提示：若原 GIF 已优化，可先 Optimize → Coalesce (unoptimize) 再处理。", "Hint: if the GIF is optimized, try Optimize → Coalesce (unoptimize) first.")
    val hintCropCoalesce: String get() = t("提示：若原 GIF 已优化，可改用 ImageMagick + coalesce。", "Hint: if the GIF is optimized, try ImageMagick + coalesce.")

    val errGifskiExplode: String get() = t("拆帧失败。", "Frame extraction failed.")
    val errGifskiNoFrames: String get() = t("拆帧失败：未生成帧文件。", "Frame extraction failed: no frames generated.")
    val errGifskiEncode: String get() = t("gifski 编码失败。", "gifski encoding failed.")
    val errVipsSize: String get() = t("无法读取帧尺寸。", "Cannot read frame dimensions.")
    val errVipsJoin: String get() = t("libvips 拼接失败。", "libvips join failed.")
    val errVipsSave: String get() = t("libvips 保存 GIF 失败。", "libvips GIF save failed.")
}

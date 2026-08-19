package com.ominigifmaker.state

/**
 * 图像处理任务的三态状态反馈（无进度条）。
 *
 * UI 仅做「处理中 → 成功 / 失败」两态展示：
 * [Running] 渲染加载态，[Success]/[Failed] 渲染结果或错误。
 */
sealed interface TaskStatus {
    data object Idle : TaskStatus
    data object Running : TaskStatus
    data class Success(val outputPath: String) : TaskStatus
    data class Failed(val message: String) : TaskStatus
}

package com.ominigifmaker.state

/**
 * 应用左侧导航栏的选项卡集合。
 *
 * [isPhaseOne] 为一期功能（阶段三实现业务逻辑）；为 `false` 者为二期预留占位路由。
 */
enum class AppTab(
    val label: String,
    val isPhaseOne: Boolean,
) {
    Resize("Resize", true),
    Crop("Crop", true),
    Rotate("Rotate", true),
    Reverse("Reverse", true),
    Speed("Speed", true),
    Optimize("Optimize", true),
    Frames("Frames", true),

    // 二期预留（占位）
    Effects("Effects", false),
    AddText("Add text", false),
    Censor("Censor", false),
    AddImage("Add image", false),
    Cut("Cut", false),
    Split("Split", false),
}

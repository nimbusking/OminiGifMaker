package com.ominigifmaker.state

/** 应用左侧导航目的地：功能模块 Tab 或应用级页面（设置 / 关于）。 */
sealed interface AppPage {
    /** 一期 / 二期功能模块。 */
    data class Module(val tab: AppTab) : AppPage

    data object Settings : AppPage
    data object About : AppPage
}

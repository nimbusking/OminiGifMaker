package com.ominigifmaker.ui.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ominigifmaker.state.AppTab
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.PlaceholderContent

/** 打码模块（二期预留占位）。 */
@Composable
fun CensorTab(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    PlaceholderContent(
        title = strings.tabLabel(AppTab.Censor),
        description = strings.phaseTwoPlaceholder,
        modifier = modifier,
    )
}

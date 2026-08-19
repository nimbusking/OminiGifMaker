package com.ominigifmaker.ui.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ominigifmaker.state.AppState

/** 应用根布局：左侧导航栏 + 右侧主工作区。 */
@Composable
fun AppLayout(appState: AppState) {
    val selectedPage by appState.selectedPage.collectAsState()
    Row(Modifier.fillMaxSize()) {
        AppNavigationRail(
            selectedPage = selectedPage,
            onPageSelected = appState::selectPage,
        )
        MainContentArea(
            appState = appState,
            modifier = Modifier.weight(1f),
        )
    }
}

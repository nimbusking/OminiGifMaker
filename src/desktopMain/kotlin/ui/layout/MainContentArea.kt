package com.ominigifmaker.ui.layout

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.ominigifmaker.core.metadata.GifMetaDataReader
import com.ominigifmaker.state.AppPage
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppTab
import com.ominigifmaker.state.MetaDataStatus
import com.ominigifmaker.ui.components.FileMetaDataPanel
import com.ominigifmaker.ui.components.ResultPreview
import com.ominigifmaker.ui.pages.AboutPage
import com.ominigifmaker.ui.pages.SettingsPage
import com.ominigifmaker.ui.tabs.AddImageTab
import com.ominigifmaker.ui.tabs.AddTextTab
import com.ominigifmaker.ui.tabs.CensorTab
import com.ominigifmaker.ui.tabs.CropTab
import com.ominigifmaker.ui.tabs.CutTab
import com.ominigifmaker.ui.tabs.EffectsTab
import com.ominigifmaker.ui.tabs.FramesTab
import com.ominigifmaker.ui.tabs.OptimizeTab
import com.ominigifmaker.ui.tabs.ResizeTab
import com.ominigifmaker.ui.tabs.ReverseTab
import com.ominigifmaker.ui.tabs.RotateTab
import com.ominigifmaker.ui.tabs.SpeedTab
import com.ominigifmaker.ui.tabs.SplitTab
import kotlinx.coroutines.launch
import java.io.File

/**
 * 主工作区：按当前导航目的地分发。
 * - 功能模块 → 顶部文件元数据区 + 中部 Tab 内容（Crossfade 切换）+ 底部结果展示区。
 * - Settings / About → 独立应用级页面。
 */
@Composable
fun MainContentArea(appState: AppState, modifier: Modifier = Modifier) {
    val selectedPage by appState.selectedPage.collectAsState()
    when (val page = selectedPage) {
        AppPage.Settings -> SettingsPage(appState, modifier)
        AppPage.About -> AboutPage(modifier)
        is AppPage.Module -> ModuleWorkspace(appState, page.tab, modifier)
    }
}

@Composable
private fun ModuleWorkspace(appState: AppState, tab: AppTab, modifier: Modifier) {
    val sourcePath by appState.sourceGifPath.collectAsState()
    val metaDataStatus by appState.metaDataStatus.collectAsState()
    val taskStatus by appState.taskStatus.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        FileMetaDataPanel(
            sourcePath = sourcePath,
            metaDataStatus = metaDataStatus,
            onFilePicked = { path ->
                appState.setSourceGif(path)
                scope.launch { loadMetaData(appState, path) }
            },
            onClear = { appState.setSourceGif(null) },
        )

        Crossfade(
            targetState = tab,
            modifier = Modifier.weight(1f),
        ) { activeTab ->
            Box(Modifier.fillMaxSize()) {
                TabContent(activeTab, appState)
            }
        }

        ResultPreview(taskStatus = taskStatus)
    }
}

@Composable
private fun TabContent(tab: AppTab, appState: AppState) {
    when (tab) {
        AppTab.Resize -> ResizeTab(appState)
        AppTab.Crop -> CropTab(appState)
        AppTab.Rotate -> RotateTab(appState)
        AppTab.Reverse -> ReverseTab(appState)
        AppTab.Speed -> SpeedTab(appState)
        AppTab.Optimize -> OptimizeTab(appState)
        AppTab.Frames -> FramesTab(appState)
        AppTab.Effects -> EffectsTab()
        AppTab.AddText -> AddTextTab()
        AppTab.Censor -> CensorTab()
        AppTab.AddImage -> AddImageTab()
        AppTab.Cut -> CutTab()
        AppTab.Split -> SplitTab()
    }
}

/** 选中源文件后异步读取元数据并回填状态。 */
private suspend fun loadMetaData(appState: AppState, path: String) {
    appState.setMetaDataStatus(MetaDataStatus.Loading)
    val status = try {
        MetaDataStatus.Ready(GifMetaDataReader.read(File(path)))
    } catch (e: Exception) {
        MetaDataStatus.Error(e.message ?: "读取元数据失败")
    }
    appState.setMetaDataStatus(status)
}

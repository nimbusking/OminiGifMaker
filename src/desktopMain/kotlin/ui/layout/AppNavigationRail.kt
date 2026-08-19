package com.ominigifmaker.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ominigifmaker.state.AppPage
import com.ominigifmaker.state.AppTab
import com.ominigifmaker.ui.LocalAppStrings

/**
 * 左侧导航栏：一期模块（Resize…Frames）路由 + 二期模块（Effects…Split）占位路由，
 * 底部固定「Settings / About」应用级页面入口。
 */
@Composable
fun AppNavigationRail(
    selectedPage: AppPage,
    onPageSelected: (AppPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier.fillMaxHeight().width(88.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = selectedPage == AppPage.Module(tab),
                        onClick = { onPageSelected(AppPage.Module(tab)) },
                        icon = { Icon(tab.icon(), contentDescription = strings.tabLabel(tab)) },
                        label = { Text(strings.tabLabel(tab)) },
                        alwaysShowLabel = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            HorizontalDivider(Modifier.fillMaxWidth())

            NavigationRailItem(
                selected = selectedPage == AppPage.Settings,
                onClick = { onPageSelected(AppPage.Settings) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = strings.settings) },
                label = { Text(strings.settings) },
                alwaysShowLabel = true,
                modifier = Modifier.fillMaxWidth(),
            )
            NavigationRailItem(
                selected = selectedPage == AppPage.About,
                onClick = { onPageSelected(AppPage.About) },
                icon = { Icon(Icons.Filled.Info, contentDescription = strings.about) },
                label = { Text(strings.about) },
                alwaysShowLabel = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 各 Tab 的导航图标映射（UI 层持有，避免 commonMain 状态依赖图标库）。 */
private fun AppTab.icon(): ImageVector = when (this) {
    AppTab.Resize -> Icons.Filled.AspectRatio
    AppTab.Crop -> Icons.Filled.Crop
    AppTab.Rotate -> Icons.Filled.Rotate90DegreesCcw
    AppTab.Reverse -> Icons.Filled.FastRewind
    AppTab.Speed -> Icons.Filled.Speed
    AppTab.Optimize -> Icons.Filled.AutoFixHigh
    AppTab.Frames -> Icons.Filled.Animation
    AppTab.Effects -> Icons.Filled.AutoAwesome
    AppTab.AddText -> Icons.Filled.TextFields
    AppTab.Censor -> Icons.Filled.BlurOn
    AppTab.AddImage -> Icons.Filled.AddPhotoAlternate
    AppTab.Cut -> Icons.Filled.ContentCut
    AppTab.Split -> Icons.AutoMirrored.Filled.CallSplit
}

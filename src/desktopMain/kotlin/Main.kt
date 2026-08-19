package com.ominigifmaker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ominigifmaker.core.engine.EngineExtractor

fun main() {
    // 引擎二进制解压：在主窗口加载前完成。
    EngineExtractor.extractAllIfNeeded()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "OminiGifMaker",
            state = rememberWindowState(width = 1000.dp, height = 700.dp),
        ) {
            // 占位内容：阶段二替换为 NavigationRail + MainContentArea。
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("OminiGifMaker")
                    }
                }
            }
        }
    }
}

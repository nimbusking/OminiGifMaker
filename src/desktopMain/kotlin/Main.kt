package com.ominigifmaker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ominigifmaker.core.engine.EngineExtractor
import com.ominigifmaker.core.settings.SettingsStore
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.layout.AppLayout
import org.jetbrains.skia.Image as SkiaImage

fun main() {
    // 引擎二进制解压：在主窗口加载前完成。
    EngineExtractor.extractAllIfNeeded()

    application {
        val appState = remember {
            val s = AppState(SettingsStore)
            // 首次运行：未保存语言偏好时按系统语言初始化
            if (SettingsStore.getString("app.language", "").isEmpty()) {
                val sys = if (System.getProperty("user.language", "").lowercase().startsWith("zh")) {
                    Language.ZH
                } else {
                    Language.EN
                }
                s.setLanguage(sys)
            }
            s
        }
        val windowIcon = remember { loadWindowIcon() }
        val language by appState.language.collectAsState()

        Window(
            onCloseRequest = ::exitApplication,
            title = "OminiGifMaker",
            icon = windowIcon,
            state = rememberWindowState(width = 1280.dp, height = 820.dp),
        ) {
            CompositionLocalProvider(LocalAppStrings provides AppStrings(language)) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppLayout(appState)
                    }
                }
            }
        }
    }
}

/**
 * 从资源加载应用图标，供运行时窗口标题栏 / 任务栏显示。
 *
 * `nativeDistributions` 里的 `iconFile` 仅作用于打包产物（.exe/.dmg/.deb），
 * `gradlew run` 启动的窗口需在此显式设置，否则显示默认 Java 图标。
 */
private fun loadWindowIcon(): Painter? {
    val bytes = runCatching {
        EngineExtractor::class.java.classLoader.getResourceAsStream("icons/icon.png")?.use { it.readBytes() }
    }.getOrNull() ?: return null
    return runCatching {
        BitmapPainter(SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap())
    }.getOrNull()
}

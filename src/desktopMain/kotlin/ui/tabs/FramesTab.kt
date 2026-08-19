package com.ominigifmaker.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ominigifmaker.core.command.CommandRunner
import com.ominigifmaker.core.command.FramesCommandBuilder
import com.ominigifmaker.core.metadata.GifMetaDataReader
import com.ominigifmaker.model.FramesConfig
import com.ominigifmaker.model.FramesConverter
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.DropdownSelector
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** 帧管理与合成模块。 */
@Composable
fun FramesTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.framesState
    val strings = LocalAppStrings.current
    val sourcePath by appState.sourceGifPath.collectAsState()
    val framePaths by state.framePaths.collectAsState()
    val globalDelay by state.globalDelay.collectAsState()
    val loopCount by state.loopCount.collectAsState()
    val useGlobalColormap by state.useGlobalColormap.collectAsState()
    val converter by state.converter.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.framesTitle, style = MaterialTheme.typography.titleLarge)

        OutlinedButton(onClick = {
            scope.launch {
                pickFrameFiles().let { state.addFrames(it.map(File::getAbsolutePath)) }
            }
        }) {
            Text(strings.uploadFrames)
        }

        if (framePaths.isEmpty()) {
            Text(
                strings.noFrames,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("${strings.framesCount}: ${framePaths.size}", style = MaterialTheme.typography.labelLarge)
            framePaths.forEachIndexed { index, path ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "${index + 1}. ${File(path).name}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { state.removeFrameAt(index) }) { Text(strings.remove) }
                }
            }
            TextButton(onClick = state::clearFrames) { Text(strings.clearFrames) }
        }

        NumberField(
            value = globalDelay,
            label = strings.delayTime,
            onValueChange = state::setGlobalDelay,
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(
            value = loopCount,
            label = strings.loopCount,
            onValueChange = state::setLoopCount,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useGlobalColormap, onCheckedChange = state::setUseGlobalColormap)
            Text(strings.useGlobalColormap)
        }

        DropdownSelector(
            label = strings.converter,
            options = FramesConverter.entries.toList(),
            selected = converter,
            labelOf = strings::framesConverterLabel,
            onSelected = state::setConverter,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
            Text(strings.rememberSettings)
        }

        Button(
            onClick = { executeFrames(appState, sourcePath, state.config, scope) },
            enabled = framePaths.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.makeGif)
        }
    }
}

private fun executeFrames(
    appState: AppState,
    sourcePath: String?,
    config: FramesConfig,
    scope: CoroutineScope,
) {
    val strings = AppStrings(appState.language.value)
    val error = FramesCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val base = sourcePath?.let { File(it).parentFile } ?: File(config.framePaths.first()).parentFile
    val output = File(base, "output.gif")
    scope.launch {
        appState.setTaskStatus(TaskStatus.Running)
        try {
            if (config.converter == FramesConverter.LIBVIPS) {
                runVips(appState, config, output)
            } else {
                val result = CommandRunner.run(FramesCommandBuilder.buildImageMagick(config.framePaths, output, config))
                appState.setTaskStatus(
                    if (result.isSuccess) TaskStatus.Success(output.absolutePath)
                    else TaskStatus.Failed(result.stderr.ifBlank { strings.processFailed(result.exitCode) })
                )
            }
        } catch (e: Exception) {
            appState.setTaskStatus(TaskStatus.Failed(e.message ?: strings.executionFailed))
        }
    }
}

/** libvips 合成：arrayjoin 纵向拼接 → gifsave。 */
private suspend fun runVips(appState: AppState, config: FramesConfig, output: File) {
    val strings = AppStrings(appState.language.value)
    val first = File(config.framePaths.first())
    val pageHeight = try {
        GifMetaDataReader.read(first).height
    } catch (e: Exception) {
        0
    }
    if (pageHeight <= 0) {
        appState.setTaskStatus(TaskStatus.Failed(strings.errVipsSize))
        return
    }
    val joined = File.createTempFile("ominigif_join", ".v")
    try {
        val r1 = CommandRunner.run(FramesCommandBuilder.buildVipsJoin(config.framePaths, joined))
        if (!r1.isSuccess) {
            appState.setTaskStatus(TaskStatus.Failed(r1.stderr.ifBlank { strings.errVipsJoin }))
            return
        }
        val r2 = CommandRunner.run(FramesCommandBuilder.buildVipsSave(joined, output, pageHeight))
        appState.setTaskStatus(
            if (r2.isSuccess) TaskStatus.Success(output.absolutePath)
            else TaskStatus.Failed(r2.stderr.ifBlank { strings.errVipsSave })
        )
    } finally {
        joined.delete()
    }
}

/** 弹出多选文件对话框选择帧图像。 */
private suspend fun pickFrameFiles(): List<File> = withContext(Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "选择帧图像", FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.setFilenameFilter { _, name ->
        val n = name.lowercase()
        n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") ||
            n.endsWith(".gif") || n.endsWith(".webp") || n.endsWith(".bmp")
    }
    dialog.isVisible = true
    dialog.files?.toList() ?: emptyList()
}

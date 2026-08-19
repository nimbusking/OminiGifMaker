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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ominigifmaker.core.command.CommandRunner
import com.ominigifmaker.core.command.EngineCommand
import com.ominigifmaker.core.command.OptimizeCommandBuilder
import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.model.OptimizeConfig
import com.ominigifmaker.model.OptimizeMethod
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.DropdownSelector
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import kotlin.math.roundToInt

/** 优化与压缩模块。 */
@Composable
fun OptimizeTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.optimizeState
    val strings = LocalAppStrings.current
    val sourcePath by appState.sourceGifPath.collectAsState()
    val method by state.method.collectAsState()
    val lossyLevel by state.lossyLevel.collectAsState()
    val colors by state.colors.collectAsState()
    val eliminateLocalTables by state.eliminateLocalTables.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.optimizeTitle, style = MaterialTheme.typography.titleLarge)

        DropdownSelector(
            label = strings.optimizationMethod,
            options = OptimizeMethod.entries.toList(),
            selected = method,
            labelOf = strings::optimizeMethodLabel,
            onSelected = state::setMethod,
        )

        if (method == OptimizeMethod.LOSSY || method == OptimizeMethod.COMBINED) {
            NumberField(
                value = lossyLevel,
                label = strings.compressionLevel,
                onValueChange = state::setLossyLevel,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (method == OptimizeMethod.COLOR_REDUCTION || method == OptimizeMethod.COLOR_REDUCTION_DITHER) {
            NumberField(
                value = colors,
                label = strings.colors,
                onValueChange = state::setColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = eliminateLocalTables, onCheckedChange = state::setEliminateLocalTables)
            Text(strings.eliminateLocalTables)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
            Text(strings.rememberSettings)
        }

        Button(
            onClick = { executeOptimize(appState, sourcePath, state.config, scope) },
            enabled = sourcePath != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.optimizeButton)
        }
    }
}

private fun executeOptimize(
    appState: AppState,
    sourcePath: String?,
    config: OptimizeConfig,
    scope: CoroutineScope,
) {
    val strings = AppStrings(appState.language.value)
    if (sourcePath == null) {
        appState.setTaskStatus(TaskStatus.Failed(strings.selectSourceFirst))
        return
    }
    val error = OptimizeCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val source = File(sourcePath)
    val output = File(source.parentFile, source.nameWithoutExtension + "_optimized.gif")
    scope.launch {
        appState.setTaskStatus(TaskStatus.Running)
        try {
            if (config.method == OptimizeMethod.REENCODE_GIFSKI) {
                runGifski(appState, source, output)
            } else {
                val result = CommandRunner.run(OptimizeCommandBuilder.build(source, output, config))
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

/** gifski 重编码：拆帧（magick → PNG）→ gifski。 */
private suspend fun runGifski(appState: AppState, source: File, output: File) {
    val strings = AppStrings(appState.language.value)
    val tmpDir = Files.createTempDirectory("ominigif_frames").toFile()
    try {
        val explode = EngineCommand(
            EngineType.IMAGEMAGICK,
            listOf(source.absolutePath, "-coalesce", File(tmpDir, "frame_%03d.png").absolutePath),
        )
        val r1 = CommandRunner.run(explode)
        if (!r1.isSuccess) {
            appState.setTaskStatus(TaskStatus.Failed(r1.stderr.ifBlank { strings.errGifskiExplode }))
            return
        }
        val pngs = tmpDir.listFiles { f -> f.name.startsWith("frame_") && f.name.endsWith(".png") }
            ?.sortedBy { it.name } ?: emptyList()
        if (pngs.isEmpty()) {
            appState.setTaskStatus(TaskStatus.Failed(strings.errGifskiNoFrames))
            return
        }
        val fps = appState.metaData?.let {
            if (it.duration > 0 && it.frameCount > 0) it.frameCount / it.duration else null
        }?.roundToInt()?.coerceAtLeast(1) ?: 10

        val args = buildList {
            add("--fps")
            add(fps.toString())
            add("--output")
            add(output.absolutePath)
            pngs.forEach { add(it.absolutePath) }
        }
        val r2 = CommandRunner.run(EngineCommand(EngineType.GIFSKI, args))
        appState.setTaskStatus(
            if (r2.isSuccess) TaskStatus.Success(output.absolutePath)
            else TaskStatus.Failed(r2.stderr.ifBlank { strings.errGifskiEncode })
        )
    } finally {
        tmpDir.deleteRecursively()
    }
}

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
import com.ominigifmaker.core.command.ReverseCommandBuilder
import com.ominigifmaker.core.engine.ProcessResult
import com.ominigifmaker.model.ReverseConfig
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** 倒放与播放控制模块。 */
@Composable
fun ReverseTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.reverseState
    val strings = LocalAppStrings.current
    val sourcePath by appState.sourceGifPath.collectAsState()
    val reverse by state.reverse.collectAsState()
    val boomerang by state.boomerang.collectAsState()
    val loopCount by state.loopCount.collectAsState()
    val addTimer by state.addTimer.collectAsState()
    val flipVertical by state.flipVertical.collectAsState()
    val flipHorizontal by state.flipHorizontal.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.reverseTitle, style = MaterialTheme.typography.titleLarge)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = reverse, onCheckedChange = state::setReverse)
            Text(strings.reverse)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = boomerang, onCheckedChange = state::setBoomerang)
            Text(strings.boomerang)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = addTimer, onCheckedChange = state::setAddTimer)
            Text(strings.addTimer)
        }

        NumberField(
            value = loopCount,
            label = strings.loopCount,
            onValueChange = state::setLoopCount,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(strings.flip, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = flipVertical, onCheckedChange = state::setFlipVertical)
            Text(strings.flipVertical)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = flipHorizontal, onCheckedChange = state::setFlipHorizontal)
            Text(strings.flipHorizontal)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
            Text(strings.rememberSettings)
        }

        Button(
            onClick = { executeReverse(appState, sourcePath, state.config, scope) },
            enabled = sourcePath != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.submit)
        }
    }
}

private fun executeReverse(
    appState: AppState,
    sourcePath: String?,
    config: ReverseConfig,
    scope: CoroutineScope,
) {
    val strings = AppStrings(appState.language.value)
    if (sourcePath == null) {
        appState.setTaskStatus(TaskStatus.Failed(strings.selectSourceFirst))
        return
    }
    val error = ReverseCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val source = File(sourcePath)
    val output = File(source.parentFile, source.nameWithoutExtension + "_reversed.gif")
    scope.launch {
        appState.setTaskStatus(TaskStatus.Running)
        try {
            val hasGifsicleOp = config.reverse || config.boomerang || config.flipVertical ||
                config.flipHorizontal || config.loopCount.isNotBlank()

            if (config.addTimer) {
                val timerInput = if (hasGifsicleOp) {
                    val tmp = File(source.parentFile, source.nameWithoutExtension + "_tmp.gif")
                    val r1 = CommandRunner.run(ReverseCommandBuilder.buildGifsicle(source, tmp, config))
                    if (!r1.isSuccess) {
                        appState.setTaskStatus(failedFrom(strings, r1))
                        return@launch
                    }
                    tmp
                } else {
                    source
                }
                val r2 = CommandRunner.run(ReverseCommandBuilder.buildTimerOverlay(timerInput, output))
                if (timerInput != source) timerInput.delete()
                appState.setTaskStatus(
                    if (r2.isSuccess) TaskStatus.Success(output.absolutePath)
                    else failedFrom(strings, r2)
                )
            } else {
                val r = CommandRunner.run(ReverseCommandBuilder.buildGifsicle(source, output, config))
                appState.setTaskStatus(
                    if (r.isSuccess) TaskStatus.Success(output.absolutePath) else failedFrom(strings, r)
                )
            }
        } catch (e: Exception) {
            appState.setTaskStatus(TaskStatus.Failed(e.message ?: strings.executionFailed))
        }
    }
}

private fun failedFrom(strings: AppStrings, result: ProcessResult): TaskStatus =
    TaskStatus.Failed(result.stderr.ifBlank { strings.processFailed(result.exitCode) })

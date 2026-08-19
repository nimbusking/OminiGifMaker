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
import com.ominigifmaker.core.command.SpeedCommandBuilder
import com.ominigifmaker.model.SpeedConfig
import com.ominigifmaker.model.SpeedMode
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.DropdownSelector
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** 速度调整模块。 */
@Composable
fun SpeedTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.speedState
    val strings = LocalAppStrings.current
    val sourcePath by appState.sourceGifPath.collectAsState()
    val mode by state.mode.collectAsState()
    val value by state.value.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.speedTitle, style = MaterialTheme.typography.titleLarge)

        DropdownSelector(
            label = strings.speedMode,
            options = SpeedMode.entries.toList(),
            selected = mode,
            labelOf = strings::speedModeLabel,
            onSelected = state::setMode,
        )

        NumberField(
            value = value,
            label = if (mode == SpeedMode.PERCENT) strings.targetSpeed else strings.frameDelay,
            onValueChange = state::setValue,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
            Text(strings.rememberSettings)
        }

        Button(
            onClick = { executeSpeed(appState, sourcePath, state.config, scope) },
            enabled = sourcePath != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.changeSpeed)
        }
    }
}

private fun executeSpeed(
    appState: AppState,
    sourcePath: String?,
    config: SpeedConfig,
    scope: CoroutineScope,
) {
    val strings = AppStrings(appState.language.value)
    if (sourcePath == null) {
        appState.setTaskStatus(TaskStatus.Failed(strings.selectSourceFirst))
        return
    }
    val error = SpeedCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val source = File(sourcePath)
    val output = File(source.parentFile, source.nameWithoutExtension + "_speed.gif")
    scope.launch {
        appState.setTaskStatus(TaskStatus.Running)
        try {
            val fps = appState.metaData?.let {
                if (it.duration > 0 && it.frameCount > 0) it.frameCount / it.duration else null
            } ?: 10.0
            val result = CommandRunner.run(SpeedCommandBuilder.build(source, output, config, fps))
            if (result.isSuccess) {
                appState.setTaskStatus(TaskStatus.Success(output.absolutePath))
            } else {
                val message = result.stderr.ifBlank { strings.processFailed(result.exitCode) }
                appState.setTaskStatus(TaskStatus.Failed(message))
            }
        } catch (e: Exception) {
            appState.setTaskStatus(TaskStatus.Failed(e.message ?: strings.executionFailed))
        }
    }
}

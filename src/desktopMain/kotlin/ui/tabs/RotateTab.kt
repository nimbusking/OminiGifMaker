package com.ominigifmaker.ui.tabs

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ominigifmaker.core.command.CommandRunner
import com.ominigifmaker.core.command.RotateCommandBuilder
import com.ominigifmaker.model.RotateConfig
import com.ominigifmaker.model.RotateMode
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** 旋转与翻转模块。 */
@Composable
fun RotateTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.rotateState
    val strings = LocalAppStrings.current
    val sourcePath by appState.sourceGifPath.collectAsState()
    val flipVertical by state.flipVertical.collectAsState()
    val flipHorizontal by state.flipHorizontal.collectAsState()
    val rotateMode by state.rotateMode.collectAsState()
    val customDegrees by state.customDegrees.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.rotateTitle, style = MaterialTheme.typography.titleLarge)

        Text(strings.flip, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = flipVertical, onCheckedChange = state::setFlipVertical)
            Text(strings.flipVertical)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = flipHorizontal, onCheckedChange = state::setFlipHorizontal)
            Text(strings.flipHorizontal)
        }

        Text(strings.rotate, style = MaterialTheme.typography.titleMedium)
        RotateMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.setRotateMode(mode) }
                    .padding(vertical = 2.dp),
            ) {
                RadioButton(selected = rotateMode == mode, onClick = null)
                Text(strings.rotateModeLabel(mode))
            }
        }

        if (rotateMode == RotateMode.CUSTOM) {
            NumberField(
                value = customDegrees,
                label = strings.degrees,
                onValueChange = state::setCustomDegrees,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
            Text(strings.rememberSettings)
        }

        Button(
            onClick = { executeRotate(appState, sourcePath, state.config, scope) },
            enabled = sourcePath != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.applyRotation)
        }
    }
}

private fun executeRotate(
    appState: AppState,
    sourcePath: String?,
    config: RotateConfig,
    scope: CoroutineScope,
) {
    val strings = AppStrings(appState.language.value)
    if (sourcePath == null) {
        appState.setTaskStatus(TaskStatus.Failed(strings.selectSourceFirst))
        return
    }
    val error = RotateCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val source = File(sourcePath)
    val output = File(source.parentFile, source.nameWithoutExtension + "_rotated.gif")
    scope.launch {
        appState.setTaskStatus(TaskStatus.Running)
        try {
            val result = CommandRunner.run(RotateCommandBuilder.build(source, output, config))
            if (result.isSuccess) {
                appState.setTaskStatus(TaskStatus.Success(output.absolutePath))
            } else {
                val message = result.stderr.ifBlank { strings.processFailed(result.exitCode) }
                appState.setTaskStatus(TaskStatus.Failed("$message\n${strings.hintUseCoalesce}"))
            }
        } catch (e: Exception) {
            appState.setTaskStatus(TaskStatus.Failed(e.message ?: strings.executionFailed))
        }
    }
}

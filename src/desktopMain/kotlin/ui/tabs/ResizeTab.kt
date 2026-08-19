package com.ominigifmaker.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.ominigifmaker.core.command.ResizeCommandBuilder
import com.ominigifmaker.model.ResizeConfig
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.DropdownSelector
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** 调整大小模块：尺寸/百分比输入 + 处理方法选择 + 执行。 */
@Composable
fun ResizeTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.resizeState
    val strings = LocalAppStrings.current
    val width by state.width.collectAsState()
    val height by state.height.collectAsState()
    val percentage by state.percentage.collectAsState()
    val method by state.method.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val sourcePath by appState.sourceGifPath.collectAsState()

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.resizeTitle, style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberField(
                value = width,
                label = strings.width,
                onValueChange = state::setWidth,
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = height,
                label = strings.height,
                onValueChange = state::setHeight,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = strings.resizeHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        NumberField(
            value = percentage,
            label = strings.percentage,
            onValueChange = state::setPercentage,
            modifier = Modifier.fillMaxWidth(),
        )

        DropdownSelector(
            label = strings.resizeMethod,
            options = com.ominigifmaker.model.ResizeMethod.entries.toList(),
            selected = method,
            labelOf = strings::resizeMethodLabel,
            onSelected = state::setMethod,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
            Text(strings.rememberSettings)
        }

        Button(
            onClick = { executeResize(appState, sourcePath, state.config, scope) },
            enabled = sourcePath != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.resizeButton)
        }
    }
}

private fun executeResize(
    appState: AppState,
    sourcePath: String?,
    config: ResizeConfig,
    scope: CoroutineScope,
) {
    val strings = AppStrings(appState.language.value)
    if (sourcePath == null) {
        appState.setTaskStatus(TaskStatus.Failed(strings.selectSourceFirst))
        return
    }
    val error = ResizeCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val source = File(sourcePath)
    val output = File(source.parentFile, source.nameWithoutExtension + "_resized.gif")
    scope.launch {
        appState.setTaskStatus(TaskStatus.Running)
        try {
            val result = CommandRunner.run(ResizeCommandBuilder.build(source, output, config))
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

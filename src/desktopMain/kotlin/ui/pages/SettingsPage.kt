package com.ominigifmaker.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ominigifmaker.core.settings.SettingsStore
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.Language
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.DropdownSelector

/** 设置页：语言切换、引擎解压目录、清除已保存设置。 */
@Composable
fun SettingsPage(appState: AppState, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    val language by appState.language.collectAsState()
    var engineDir by remember { mutableStateOf(SettingsStore.engineExtractionDir) }
    var cleared by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(strings.settingsTitle, style = MaterialTheme.typography.titleLarge)

        DropdownSelector(
            label = strings.language,
            options = Language.entries.toList(),
            selected = language,
            labelOf = { it.displayName },
            onSelected = appState::setLanguage,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.engine, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = engineDir,
                onValueChange = { engineDir = it },
                label = { Text(strings.engineDir) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                strings.engineDirHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    SettingsStore.engineExtractionDir = engineDir
                    cleared = false
                },
            ) {
                Text(strings.save)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.data, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.rememberHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    SettingsStore.clear()
                    engineDir = SettingsStore.engineExtractionDir
                    cleared = true
                },
            ) {
                Text(strings.clearAllSettings)
            }
            if (cleared) {
                Text(
                    strings.cleared,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

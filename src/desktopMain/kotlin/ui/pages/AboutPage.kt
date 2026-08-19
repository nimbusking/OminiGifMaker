package com.ominigifmaker.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ominigifmaker.ui.LocalAppStrings

/** 关于页：应用信息与许可证。 */
@Composable
fun AboutPage(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.aboutTitle, style = MaterialTheme.typography.titleLarge)
        Text(strings.appName, style = MaterialTheme.typography.headlineSmall)
        Text(strings.version, style = MaterialTheme.typography.bodyMedium)
        Text(
            strings.aboutDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(strings.license, style = MaterialTheme.typography.bodyMedium)
        Text(
            strings.thirdParty,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

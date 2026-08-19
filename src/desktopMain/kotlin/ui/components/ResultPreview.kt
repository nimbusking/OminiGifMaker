package com.ominigifmaker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings

/**
 * 跨模块共享的结果展示区（三态）。
 *
 * 根据 [TaskStatus] 渲染：处理中（加载态）、成功（动画预览处理后的 GIF）、失败（错误提示）。
 */
@Composable
fun ResultPreview(taskStatus: TaskStatus, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(strings.result, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            when (taskStatus) {
                TaskStatus.Idle -> Text(
                    text = strings.resultPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TaskStatus.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.processing)
                }

                is TaskStatus.Success -> SuccessContent(strings, taskStatus.outputPath)

                is TaskStatus.Failed -> Text(
                    text = taskStatus.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(strings: AppStrings, outputPath: String) {
    Column {
        Text(
            text = "${strings.outputFile}: $outputPath",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedGifPreview(
                path = outputPath,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

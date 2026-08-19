package com.ominigifmaker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ominigifmaker.model.GifMetaData
import com.ominigifmaker.state.MetaDataStatus
import com.ominigifmaker.ui.LocalAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * 顶部共享的文件元数据展示区：文件选择逻辑 + ffprobe 元数据展示。
 *
 * 选择文件后通过 [onFilePicked] 回调上层，由上层负责调用 [com.ominigifmaker.core.metadata.GifMetaDataReader]，
 * 本组件只负责渲染 [metaDataStatus] 的四态。
 */
@Composable
fun FileMetaDataPanel(
    sourcePath: String?,
    metaDataStatus: MetaDataStatus,
    onFilePicked: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(strings.sourceFile, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    pickGifFile()?.let { onFilePicked(it.absolutePath) }
                }
            }) {
                Text(strings.chooseGif)
            }
            if (sourcePath != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onClear) {
                    Text(strings.clear)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (val status = metaDataStatus) {
            MetaDataStatus.Idle -> Text(
                text = strings.noFileSelected,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MetaDataStatus.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(strings.readingMetadata)
            }

            is MetaDataStatus.Error -> Text(
                text = status.message,
                color = MaterialTheme.colorScheme.error,
            )

            is MetaDataStatus.Ready -> MetaDataGrid(strings, data = status.data, path = sourcePath)
        }
    }
}

@Composable
private fun MetaDataGrid(strings: com.ominigifmaker.state.AppStrings, data: GifMetaData, path: String?) {
    Column {
        if (path != null) {
            Text(
                text = "${strings.path}: $path",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            MetaDataItem(strings.dimensions, "${data.width} × ${data.height}")
            MetaDataItem(strings.frames, data.frameCount.toString())
            MetaDataItem(strings.format, data.format.ifEmpty { "—" })
            MetaDataItem(strings.duration, "%.2f s".format(data.duration))
            MetaDataItem(strings.size, formatBytes(data.size))
        }
    }
}

@Composable
private fun MetaDataItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 将字节数格式化为可读体积字符串。 */
private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    return when {
        bytes >= mb -> "%.2f MB".format(bytes / mb)
        bytes >= kb -> "%.1f KB".format(bytes / kb)
        else -> "$bytes B"
    }
}

/** 弹出原生文件选择对话框并返回选中的 GIF 文件（取消返回 null）。 */
private suspend fun pickGifFile(): File? = withContext(Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "选择 GIF 文件", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.lowercase().endsWith(".gif") }
    dialog.isVisible = true
    val dir = dialog.directory
    val name = dialog.file
    if (dir != null && name != null) File(dir, name) else null
}

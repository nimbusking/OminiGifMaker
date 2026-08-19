package com.ominigifmaker.ui.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ominigifmaker.core.command.CommandRunner
import com.ominigifmaker.core.command.FramesCommandBuilder
import com.ominigifmaker.core.engine.EngineExtractor
import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.core.metadata.GifMetaDataReader
import com.ominigifmaker.model.FrameEntry
import com.ominigifmaker.model.FramesConfig
import com.ominigifmaker.model.FramesConverter
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.FramesState
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.DropdownSelector
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

/** 跳过/启用按钮颜色。 */
private val SkipRed = Color(0xFFD32F2F)
private val CopyBlue = Color(0xFF1976D2)

/** 帧管理与合成模块。 */
@Composable
fun FramesTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.framesState
    val strings = LocalAppStrings.current
    val frames by state.frames.collectAsState()
    val globalDelay by state.globalDelay.collectAsState()
    val loopCount by state.loopCount.collectAsState()
    val useGlobalColormap by state.useGlobalColormap.collectAsState()
    val converter by state.converter.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val sourcePath by appState.sourceGifPath.collectAsState()
    val scope = rememberCoroutineScope()
    var exploding by remember { mutableStateOf(false) }
    var explodeError by remember { mutableStateOf<String?>(null) }

    // 复用顶部共享「选择 GIF」：源 GIF 变化时拆帧 / 清空。
    LaunchedEffect(sourcePath) {
        val path = sourcePath
        if (path == state.sourceGifPath.value) return@LaunchedEffect
        if (path == null) {
            state.clearFrames()
            explodeError = null
            return@LaunchedEffect
        }
        exploding = true
        explodeError = null
        explodeError = try {
            val paths = explodeGif(File(path), strings)
            state.setFrames(paths)
            state.setSourceGif(path)
            null
        } catch (e: Exception) {
            e.message ?: strings.errExplode
        }
        exploding = false
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(strings.framesTitle, style = MaterialTheme.typography.titleLarge)
        }

        if (exploding) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(strings.processing, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        explodeError?.let { err ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (frames.isEmpty()) {
            if (!exploding) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = strings.noFrames,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val skipped = frames.count { it.skipped }
                Text(
                    text = "${strings.framesCount}: ${frames.size}    ${strings.skippedCount}: $skipped",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            items(frames.size) { index ->
                FrameCell(
                    entry = frames[index],
                    index = index,
                    strings = strings,
                    onToggleSkip = { state.toggleSkip(index) },
                    onCopy = { state.copyFrame(index) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ToggleRangeBar(state, strings)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.gifOptions, style = MaterialTheme.typography.titleMedium)
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
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
                    Text(strings.rememberSettings)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = { executeFrames(appState, state, scope) },
                    enabled = frames.any { !it.skipped },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.makeGif)
                }
            }
        }
    }
}

/** 单帧单元格：缩略图 + 序号角标 + 跳过/拷贝按钮，跳过时置灰。 */
@Composable
private fun FrameCell(
    entry: FrameEntry,
    index: Int,
    strings: AppStrings,
    onToggleSkip: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val bitmap by produceState<ImageBitmap?>(initialValue = null, entry.path) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        SkiaImage.makeFromEncoded(File(entry.path).readBytes()).toComposeImageBitmap()
                    }.getOrNull()
                }
            }
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .let { if (entry.skipped) it.alpha(0.35f) else it },
                )
            }
            if (entry.skipped) {
                Box(Modifier.matchParentSize().background(Color.Gray.copy(alpha = 0.25f)))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${index + 1}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = onToggleSkip,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (entry.skipped) MaterialTheme.colorScheme.surfaceVariant else SkipRed,
                    contentColor = if (entry.skipped) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (entry.skipped) strings.enable else strings.skip,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Button(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CopyBlue, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            ) {
                Text(strings.copy, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** 批量帧操作栏：按区间 / 按步长 跳过或启用。 */
@Composable
private fun ToggleRangeBar(state: FramesState, strings: AppStrings) {
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var everyText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.toggleRangeTitle, style = MaterialTheme.typography.titleMedium)

        // 第一行：按区间
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.from, style = MaterialTheme.typography.bodyMedium)
            NumberField(value = fromText, label = "", onValueChange = { fromText = it }, modifier = Modifier.width(88.dp))
            Text(strings.to, style = MaterialTheme.typography.bodyMedium)
            NumberField(value = toText, label = "", onValueChange = { toText = it }, modifier = Modifier.width(88.dp))
            OutlinedButton(
                onClick = {
                    val f = fromText.toIntOrNull()
                    val t = toText.toIntOrNull()
                    if (f != null && t != null) state.skipRange(f, t)
                },
            ) { Text(strings.skipRange) }
            OutlinedButton(
                onClick = {
                    val f = fromText.toIntOrNull()
                    val t = toText.toIntOrNull()
                    if (f != null && t != null) state.enableRange(f, t)
                },
            ) { Text(strings.enableRange) }
        }

        // 第二行：按步长
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.every, style = MaterialTheme.typography.bodyMedium)
            NumberField(value = everyText, label = "", onValueChange = { everyText = it }, modifier = Modifier.width(88.dp))
            Text(strings.everySuffix, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = { everyText.toIntOrNull()?.let { state.skipEveryNth(it) } },
            ) { Text(strings.skipEveryNth) }
            OutlinedButton(
                onClick = { everyText.toIntOrNull()?.let { state.enableEveryNth(it) } },
            ) { Text(strings.enableEveryNth) }
        }
    }
}

private fun executeFrames(appState: AppState, state: FramesState, scope: CoroutineScope) {
    val strings = AppStrings(appState.language.value)
    val config = state.config
    val error = FramesCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val sourceGif = state.sourceGifPath.value
    val base = sourceGif?.let { File(it).parentFile } ?: File(config.framePaths.first()).parentFile
    val name = sourceGif?.let { File(it).nameWithoutExtension } ?: "output"
    val output = File(base, "${name}_frames.gif")
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

/** 将 GIF 拆分为有序 PNG 帧路径列表；失败时抛出带本地化信息的异常。 */
private suspend fun explodeGif(gif: File, strings: AppStrings): List<String> {
    EngineExtractor.ensureExtracted(EngineType.IMAGEMAGICK)
    val dir = File.createTempFile("ominigif_frames", "").apply { delete(); mkdirs() }
    val pattern = File(dir, "frame_%03d.png")
    val result = CommandRunner.run(FramesCommandBuilder.buildExplode(gif, pattern))
    if (!result.isSuccess) {
        dir.deleteRecursively()
        throw IllegalStateException(result.stderr.ifBlank { strings.errExplode })
    }
    val frames = dir.listFiles { f -> f.isFile && f.extension.lowercase() == "png" }
        ?.sortedBy { frameIndex(it.name) }
        ?.map { it.absolutePath }
        ?: emptyList()
    if (frames.isEmpty()) {
        dir.deleteRecursively()
        throw IllegalStateException(strings.errExplodeNoFrames)
    }
    return frames
}

/** 从文件名提取帧序号用于自然排序（frame_0.png、frame_10.png、frame_100.png…）。 */
private fun frameIndex(name: String): Int =
    Regex("\\d+").find(name)?.value?.toIntOrNull() ?: Int.MAX_VALUE

package com.ominigifmaker.ui.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ominigifmaker.core.command.CommandRunner
import com.ominigifmaker.core.command.CropCommandBuilder
import com.ominigifmaker.model.CropAspectRatio
import com.ominigifmaker.model.CropBackground
import com.ominigifmaker.model.CropConfig
import com.ominigifmaker.model.CropMethod
import com.ominigifmaker.state.AppState
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.TaskStatus
import com.ominigifmaker.ui.LocalAppStrings
import com.ominigifmaker.ui.components.NumberField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/** 裁剪模块：可拖拽预览 + 裁剪参数表单 + 执行。 */
@Composable
fun CropTab(appState: AppState, modifier: Modifier = Modifier) {
    val state = appState.cropState
    val strings = LocalAppStrings.current
    val sourcePath by appState.sourceGifPath.collectAsState()
    val left by state.left.collectAsState()
    val top by state.top.collectAsState()
    val width by state.width.collectAsState()
    val height by state.height.collectAsState()
    val method by state.method.collectAsState()
    val background by state.background.collectAsState()
    val lockAspect by state.lockAspect.collectAsState()
    val autocrop by state.autocrop.collectAsState()
    val dontScaleLarge by state.dontScaleLarge.collectAsState()
    val rememberSettings by state.rememberSettings.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.cropTitle, style = MaterialTheme.typography.titleLarge)

        CropPreview(
            sourcePath = sourcePath,
            background = background,
            dontScaleLarge = dontScaleLarge,
            lockAspect = lockAspect,
            left = left.toIntOrNull() ?: 0,
            top = top.toIntOrNull() ?: 0,
            width = width.toIntOrNull() ?: 0,
            height = height.toIntOrNull() ?: 0,
            onRectChanged = state::setRect,
            onResetToFull = state::resetToFull,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(left, strings.left, state::setLeft, Modifier.weight(1f))
            NumberField(top, strings.top, state::setTop, Modifier.weight(1f))
            NumberField(width, strings.width, state::setWidth, Modifier.weight(1f))
            NumberField(height, strings.height, state::setHeight, Modifier.weight(1f))
        }

        DropdownSelector(
            label = strings.lockAspect,
            options = CropAspectRatio.entries.toList(),
            selected = lockAspect,
            labelOf = strings::cropAspectLabel,
            onSelected = state::setLockAspect,
        )

        DropdownSelector(
            label = strings.cropWith,
            options = CropMethod.entries.toList(),
            selected = method,
            labelOf = strings::cropMethodLabel,
            onSelected = state::setMethod,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = autocrop, onCheckedChange = state::setAutocrop)
            Text(strings.autocrop)
        }

        DropdownSelector(
            label = strings.background,
            options = CropBackground.entries.toList(),
            selected = background,
            labelOf = strings::cropBackgroundLabel,
            onSelected = state::setBackground,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = dontScaleLarge, onCheckedChange = state::setDontScaleLarge)
            Text(strings.dontScaleLarge)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberSettings, onCheckedChange = state::setRememberSettings)
            Text(strings.rememberSettings)
        }

        Button(
            onClick = { executeCrop(appState, sourcePath, state.config, scope) },
            enabled = sourcePath != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.cropButton)
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(labelOf(selected), modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(labelOf(opt)) },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private const val MIN_CROP_SIZE = 8

@Composable
private fun CropPreview(
    sourcePath: String?,
    background: CropBackground,
    dontScaleLarge: Boolean,
    lockAspect: CropAspectRatio,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    onRectChanged: (Int, Int, Int, Int) -> Unit,
    onResetToFull: (Int, Int) -> Unit,
) {
    val strings = LocalAppStrings.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sourcePath) {
        value = if (sourcePath == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    SkiaImage.makeFromEncoded(File(sourcePath).readBytes()).toComposeImageBitmap()
                }.getOrNull()
            }
        }
    }

    LaunchedEffect(bitmap) {
        val bmp = bitmap ?: return@LaunchedEffect
        onResetToFull(bmp.width, bmp.height)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .previewBackground(background)
            .clipToBounds(),
    ) {
        val bmp = bitmap
        if (bmp == null) {
            Text(
                strings.cropPreviewPlaceholder,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@BoxWithConstraints
        }

        val density = LocalDensity.current.density
        val imgW = bmp.width
        val imgH = bmp.height
        val fitScale = min(maxWidth.value / imgW, maxHeight.value / imgH)
        val scale = if (dontScaleLarge) 1f else fitScale.coerceAtMost(1f)
        val screenScale = scale * density

        val dispW = (imgW * scale).dp
        val dispH = (imgH * scale).dp
        val offsetX = (maxWidth - dispW) / 2
        val offsetY = (maxHeight - dispH) / 2

        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.offset(offsetX, offsetY).size(dispW, dispH),
        )

        val selX = offsetX + (left * scale).dp
        val selY = offsetY + (top * scale).dp
        val selW = (width * scale).dp
        val selH = (height * scale).dp

        Box(
            modifier = Modifier
                .offset(selX, selY)
                .size(selW, selH),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.White)
                    .background(Color.White.copy(alpha = 0.12f))
                    .pointerInput(left, top, width, height, imgW, imgH, screenScale) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dxImg = (dragAmount.x / screenScale).roundToInt()
                            val dyImg = (dragAmount.y / screenScale).roundToInt()
                            val nl = (left + dxImg).coerceIn(0, (imgW - width).coerceAtLeast(0))
                            val nt = (top + dyImg).coerceIn(0, (imgH - height).coerceAtLeast(0))
                            onRectChanged(nl, nt, width, height)
                        }
                    },
            )
            CropHandle(Alignment.TopStart, left, top, width, height, imgW, imgH, screenScale, lockAspect.ratio, onRectChanged)
            CropHandle(Alignment.TopEnd, left, top, width, height, imgW, imgH, screenScale, lockAspect.ratio, onRectChanged)
            CropHandle(Alignment.BottomStart, left, top, width, height, imgW, imgH, screenScale, lockAspect.ratio, onRectChanged)
            CropHandle(Alignment.BottomEnd, left, top, width, height, imgW, imgH, screenScale, lockAspect.ratio, onRectChanged)
        }
    }
}

@Composable
private fun BoxScope.CropHandle(
    alignment: Alignment,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    imgW: Int,
    imgH: Int,
    screenScale: Float,
    ratio: Double?,
    onRectChanged: (Int, Int, Int, Int) -> Unit,
) {
    val handleSize = 12.dp
    val half = handleSize / 2
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(
                x = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart) -half else half,
                y = if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd) -half else half,
            )
            .size(handleSize)
            .background(Color.White)
            .border(1.dp, Color.Black)
            .pointerInput(left, top, width, height, imgW, imgH, screenScale, ratio) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dxImg = (dragAmount.x / screenScale).roundToInt()
                    val dyImg = (dragAmount.y / screenScale).roundToInt()
                    var nl = left
                    var nt = top
                    var nw = width
                    var nh = height
                    when (alignment) {
                        Alignment.TopStart -> { nl += dxImg; nw -= dxImg; nt += dyImg; nh -= dyImg }
                        Alignment.TopEnd -> { nw += dxImg; nt += dyImg; nh -= dyImg }
                        Alignment.BottomStart -> { nl += dxImg; nw -= dxImg; nh += dyImg }
                        else -> { nw += dxImg; nh += dyImg }
                    }
                    if (ratio != null) {
                        nh = (nw / ratio).roundToInt()
                    }
                    nw = nw.coerceAtLeast(MIN_CROP_SIZE)
                    nh = nh.coerceAtLeast(MIN_CROP_SIZE)
                    nl = nl.coerceIn(0, (imgW - nw).coerceAtLeast(0))
                    nt = nt.coerceIn(0, (imgH - nh).coerceAtLeast(0))
                    nw = nw.coerceAtMost(imgW - nl)
                    nh = nh.coerceAtMost(imgH - nt)
                    onRectChanged(nl, nt, nw, nh)
                }
            },
    )
}

private fun Modifier.previewBackground(bg: CropBackground): Modifier = when (bg) {
    CropBackground.WHITE -> background(Color.White)
    CropBackground.BLACK -> background(Color.Black)
    CropBackground.CHECKERED -> drawBehind {
        val cell = 16.dp.toPx()
        val light = Color(0xFFE8E8E8)
        val dark = Color(0xFFC0C0C0)
        var row = 0
        var y = 0f
        while (y < size.height) {
            var col = 0
            var x = 0f
            while (x < size.width) {
                drawRect(
                    color = if ((row + col) % 2 == 0) light else dark,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                )
                x += cell
                col++
            }
            y += cell
            row++
        }
    }
}

private fun executeCrop(
    appState: AppState,
    sourcePath: String?,
    config: CropConfig,
    scope: CoroutineScope,
) {
    val strings = AppStrings(appState.language.value)
    if (sourcePath == null) {
        appState.setTaskStatus(TaskStatus.Failed(strings.selectSourceFirst))
        return
    }
    val error = CropCommandBuilder.validate(config, appState.language.value)
    if (error != null) {
        appState.setTaskStatus(TaskStatus.Failed(error))
        return
    }
    val source = File(sourcePath)
    val output = File(source.parentFile, source.nameWithoutExtension + "_cropped.gif")
    scope.launch {
        appState.setTaskStatus(TaskStatus.Running)
        try {
            val result = CommandRunner.run(CropCommandBuilder.build(source, output, config))
            if (result.isSuccess) {
                appState.setTaskStatus(TaskStatus.Success(output.absolutePath))
            } else {
                val message = result.stderr.ifBlank { strings.processFailed(result.exitCode) }
                appState.setTaskStatus(TaskStatus.Failed("$message\n${strings.hintCropCoalesce}"))
            }
        } catch (e: Exception) {
            appState.setTaskStatus(TaskStatus.Failed(e.message ?: strings.executionFailed))
        }
    }
}

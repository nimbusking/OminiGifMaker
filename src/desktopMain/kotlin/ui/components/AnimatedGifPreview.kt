package com.ominigifmaker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

/** 解码后的一帧：位图 + 显示时长（毫秒）。 */
private data class GifFrame(val bitmap: ImageBitmap, val durationMs: Long)

/**
 * 公共 GIF 动画预览：解码全部帧并按各自时长循环播放。
 *
 * 供各模块「结果预览」复用。行为：
 * - 多帧 GIF → 循环播放动画；
 * - 单帧 / 解码失败 → 退化为静态首帧。
 */
@Composable
fun AnimatedGifPreview(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val frames by produceState<List<GifFrame>>(initialValue = emptyList(), path) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeGifFrames(path) }.getOrDefault(emptyList())
        }
    }

    when {
        frames.size > 1 -> {
            var index by remember(path) { mutableStateOf(0) }
            LaunchedEffect(frames) {
                while (true) {
                    val frame = frames[index]
                    delay(frame.durationMs)
                    index = (index + 1) % frames.size
                }
            }
            Image(
                bitmap = frames[index].bitmap,
                contentDescription = null,
                modifier = modifier,
                contentScale = contentScale,
            )
        }

        frames.size == 1 -> Image(
            bitmap = frames[0].bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )

        else -> StaticPreview(path, modifier, contentScale)
    }
}

/** 静态回退：加载中或解码失败时展示首帧。 */
@Composable
private fun StaticPreview(path: String, modifier: Modifier, contentScale: ContentScale) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching { SkiaImage.makeFromEncoded(File(path).readBytes()).toComposeImageBitmap() }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

/** 使用 Skia Codec 解码 GIF 的全部帧及各自时长。 */
private fun decodeGifFrames(path: String): List<GifFrame> {
    val codec = Codec.makeFromData(Data.makeFromBytes(File(path).readBytes()))
    val frameCount = codec.frameCount
    val infos = codec.framesInfo
    return (0 until frameCount).map { i ->
        val bitmap = Bitmap().apply { allocPixels(codec.imageInfo) }
        codec.readPixels(bitmap, i, -1)
        val duration = infos.getOrNull(i)?.duration?.toLong()?.coerceAtLeast(20L) ?: 100L
        GifFrame(SkiaImage.makeFromBitmap(bitmap).toComposeImageBitmap(), duration)
    }
}

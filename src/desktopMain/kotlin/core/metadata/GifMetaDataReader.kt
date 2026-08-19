package com.ominigifmaker.core.metadata

import com.ominigifmaker.core.engine.EngineExtractor
import com.ominigifmaker.core.engine.EngineType
import com.ominigifmaker.core.engine.ProcessRunner
import com.ominigifmaker.model.GifMetaData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** 基于 ffprobe（随 FFmpeg 分发）读取 GIF 文件元数据。 */
object GifMetaDataReader {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(file: File): GifMetaData {
        EngineExtractor.ensureExtracted(EngineType.FFPROBE)
        val result = ProcessRunner.run(
            executable = EngineExtractor.executableFile(EngineType.FFPROBE),
            args = listOf(
                "-v", "error",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                file.absolutePath,
            ),
        )
        require(result.isSuccess) { "ffprobe failed: ${result.stderr}" }
        return parse(result.stdout)
    }

    private fun parse(raw: String): GifMetaData {
        val output = json.decodeFromString<FfprobeOutput>(raw)
        val stream = output.streams.firstOrNull { it.width > 0 || it.height > 0 }
            ?: output.streams.firstOrNull()
        val format = output.format
        return GifMetaData(
            size = format?.size?.toLongOrNull() ?: 0L,
            width = stream?.width ?: 0,
            height = stream?.height ?: 0,
            frameCount = stream?.nbFrames?.toIntOrNull() ?: 0,
            format = format?.formatName ?: "",
            duration = format?.duration?.toDoubleOrNull() ?: 0.0,
        )
    }

    @Serializable
    private data class FfprobeOutput(
        val streams: List<FfprobeStream> = emptyList(),
        val format: FfprobeFormat? = null,
    )

    @Serializable
    private data class FfprobeStream(
        @SerialName("width") val width: Int = 0,
        @SerialName("height") val height: Int = 0,
        @SerialName("nb_frames") val nbFrames: String = "",
    )

    @Serializable
    private data class FfprobeFormat(
        @SerialName("size") val size: String = "",
        @SerialName("format_name") val formatName: String = "",
        @SerialName("duration") val duration: String = "",
    )
}

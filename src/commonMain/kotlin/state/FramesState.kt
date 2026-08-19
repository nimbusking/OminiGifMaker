package com.ominigifmaker.state

import com.ominigifmaker.model.FrameEntry
import com.ominigifmaker.model.FramesConfig
import com.ominigifmaker.model.FramesConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Frames 模块状态（StateHolder）：帧序列 + GIF 选项。 */
class FramesState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    /** 有序帧列表（拆帧或追加所得），每帧带跳过标记。 */
    private val _frames = MutableStateFlow<List<FrameEntry>>(emptyList())
    val frames: StateFlow<List<FrameEntry>> = _frames.asStateFlow()

    /** 上传的源 GIF 路径（用于确定输出目录）。 */
    private val _sourceGifPath = MutableStateFlow<String?>(null)
    val sourceGifPath: StateFlow<String?> = _sourceGifPath.asStateFlow()

    private val _globalDelay = MutableStateFlow(if (remembered) settings?.getString(KEY_DELAY, "") ?: "" else "")
    val globalDelay: StateFlow<String> = _globalDelay.asStateFlow()

    private val _loopCount = MutableStateFlow(if (remembered) settings?.getString(KEY_LOOP, "") ?: "" else "")
    val loopCount: StateFlow<String> = _loopCount.asStateFlow()

    private val _useGlobalColormap = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_COLORMAP, false) ?: false else false)
    val useGlobalColormap: StateFlow<Boolean> = _useGlobalColormap.asStateFlow()

    private val _converter = MutableStateFlow(enumOr(settings, KEY_CONVERTER, FramesConverter.LIBVIPS))
    val converter: StateFlow<FramesConverter> = _converter.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    /** 合成配置：仅包含未跳过的帧路径。 */
    val config: FramesConfig
        get() = FramesConfig(
            framePaths = _frames.value.filterNot { it.skipped }.map { it.path },
            globalDelay = _globalDelay.value,
            loopCount = _loopCount.value,
            useGlobalColormap = _useGlobalColormap.value,
            converter = _converter.value,
        )

    // ---- 帧序列操作 ----

    /** 用拆帧结果替换当前帧列表。 */
    fun setFrames(paths: List<String>) {
        _frames.value = paths.map { FrameEntry(it) }
    }

    /** 向当前序列追加帧（保留跳过状态）。 */
    fun addFrames(paths: List<String>) {
        _frames.value = _frames.value + paths.map { FrameEntry(it) }
    }

    fun setSourceGif(path: String?) {
        _sourceGifPath.value = path
    }

    /** 切换第 [index] 帧的跳过状态。 */
    fun toggleSkip(index: Int) {
        _frames.value = _frames.value.mapIndexed { i, e ->
            if (i == index) e.copy(skipped = !e.skipped) else e
        }
    }

    /** 拷贝第 [index] 帧（在其后插入一份副本，副本默认未跳过）。 */
    fun copyFrame(index: Int) {
        val list = _frames.value
        if (index !in list.indices) return
        val copy = list[index].copy(skipped = false)
        _frames.value = list.toMutableList().apply { add(index + 1, copy) }
    }

    fun removeFrameAt(index: Int) {
        if (index !in _frames.value.indices) return
        _frames.value = _frames.value.filterIndexed { i, _ -> i != index }
    }

    fun clearFrames() {
        _frames.value = emptyList()
        _sourceGifPath.value = null
    }

    /** 跳过区间 [from]..[to]（1-based，含两端，自动归一化顺序）。 */
    fun skipRange(from: Int, to: Int) = setRange(from, to, skipped = true)

    /** 启用区间 [from]..[to]（1-based，含两端）。 */
    fun enableRange(from: Int, to: Int) = setRange(from, to, skipped = false)

    /** 跳过每第 [every] 帧（1-based 位置被 [every] 整除的帧）。 */
    fun skipEveryNth(every: Int) = setEveryNth(every, skipped = true)

    /** 启用每第 [every] 帧。 */
    fun enableEveryNth(every: Int) = setEveryNth(every, skipped = false)

    private fun setRange(from: Int, to: Int, skipped: Boolean) {
        val n = _frames.value.size
        if (n == 0) return
        val lo = minOf(from, to).coerceIn(1, n)
        val hi = maxOf(from, to).coerceIn(1, n)
        _frames.value = _frames.value.mapIndexed { i, e ->
            if ((i + 1) in lo..hi) e.copy(skipped = skipped) else e
        }
    }

    private fun setEveryNth(every: Int, skipped: Boolean) {
        if (every <= 0) return
        _frames.value = _frames.value.mapIndexed { i, e ->
            if ((i + 1) % every == 0) e.copy(skipped = skipped) else e
        }
    }

    // ---- 选项 ----

    fun setGlobalDelay(v: String) { _globalDelay.value = v; persist() }
    fun setLoopCount(v: String) { _loopCount.value = v; persist() }
    fun setUseGlobalColormap(v: Boolean) { _useGlobalColormap.value = v; persist() }
    fun setConverter(c: FramesConverter) { _converter.value = c; persist() }

    fun setRememberSettings(v: Boolean) {
        _rememberSettings.value = v
        val s = settings ?: return
        s.putBoolean(KEY_REMEMBER, v)
        if (v) persistAll(s) else clearAll(s)
    }

    private fun persist() {
        if (!_rememberSettings.value) return
        settings?.let(::persistAll)
    }

    private fun persistAll(s: KeyValueStore) {
        s.putString(KEY_DELAY, _globalDelay.value)
        s.putString(KEY_LOOP, _loopCount.value)
        s.putBoolean(KEY_COLORMAP, _useGlobalColormap.value)
        s.putString(KEY_CONVERTER, _converter.value.name)
    }

    private fun clearAll(s: KeyValueStore) {
        s.putString(KEY_DELAY, "")
        s.putString(KEY_LOOP, "")
        s.putBoolean(KEY_COLORMAP, false)
        s.putString(KEY_CONVERTER, "")
    }

    private inline fun <reified T : Enum<T>> enumOr(s: KeyValueStore?, key: String, default: T): T =
        runCatching { enumValueOf<T>(s?.getString(key, "") ?: "") }.getOrDefault(default)

    private companion object {
        const val KEY_DELAY = "frames.delay"
        const val KEY_LOOP = "frames.loop"
        const val KEY_COLORMAP = "frames.colormap"
        const val KEY_CONVERTER = "frames.converter"
        const val KEY_REMEMBER = "frames.remember"
    }
}

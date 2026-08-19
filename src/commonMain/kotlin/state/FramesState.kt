package com.ominigifmaker.state

import com.ominigifmaker.model.FramesConfig
import com.ominigifmaker.model.FramesConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Frames 模块状态（StateHolder）：帧序列 + GIF 选项。 */
class FramesState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    private val _framePaths = MutableStateFlow<List<String>>(emptyList())
    val framePaths: StateFlow<List<String>> = _framePaths.asStateFlow()

    private val _globalDelay = MutableStateFlow(if (remembered) settings?.getString(KEY_DELAY, "") ?: "" else "")
    val globalDelay: StateFlow<String> = _globalDelay.asStateFlow()

    private val _loopCount = MutableStateFlow(if (remembered) settings?.getString(KEY_LOOP, "") ?: "" else "")
    val loopCount: StateFlow<String> = _loopCount.asStateFlow()

    private val _useGlobalColormap = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_COLORMAP, false) ?: false else false)
    val useGlobalColormap: StateFlow<Boolean> = _useGlobalColormap.asStateFlow()

    private val _converter = MutableStateFlow(enumOr(settings, KEY_CONVERTER, FramesConverter.IMAGEMAGICK))
    val converter: StateFlow<FramesConverter> = _converter.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    val config: FramesConfig
        get() = FramesConfig(
            framePaths = _framePaths.value,
            globalDelay = _globalDelay.value,
            loopCount = _loopCount.value,
            useGlobalColormap = _useGlobalColormap.value,
            converter = _converter.value,
        )

    fun addFrames(paths: List<String>) {
        _framePaths.value = _framePaths.value + paths
    }

    fun removeFrameAt(index: Int) {
        if (index !in _framePaths.value.indices) return
        _framePaths.value = _framePaths.value.filterIndexed { i, _ -> i != index }
    }

    fun clearFrames() {
        _framePaths.value = emptyList()
    }

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

package com.ominigifmaker.state

import com.ominigifmaker.model.RotateConfig
import com.ominigifmaker.model.RotateMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Rotate 模块表单状态（StateHolder），支持「Remember settings」跨会话持久化。 */
class RotateState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    private val _flipVertical = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_FLIP_V, false) ?: false else false)
    val flipVertical: StateFlow<Boolean> = _flipVertical.asStateFlow()

    private val _flipHorizontal = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_FLIP_H, false) ?: false else false)
    val flipHorizontal: StateFlow<Boolean> = _flipHorizontal.asStateFlow()

    private val _rotateMode = MutableStateFlow(enumOr(settings, KEY_MODE, RotateMode.NONE))
    val rotateMode: StateFlow<RotateMode> = _rotateMode.asStateFlow()

    private val _customDegrees = MutableStateFlow(if (remembered) settings?.getString(KEY_DEGREES, "") ?: "" else "")
    val customDegrees: StateFlow<String> = _customDegrees.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    /** 当前表单快照。 */
    val config: RotateConfig
        get() = RotateConfig(
            flipVertical = _flipVertical.value,
            flipHorizontal = _flipHorizontal.value,
            rotateMode = _rotateMode.value,
            customDegrees = _customDegrees.value,
        )

    fun setFlipVertical(v: Boolean) { _flipVertical.value = v; persist() }
    fun setFlipHorizontal(v: Boolean) { _flipHorizontal.value = v; persist() }
    fun setRotateMode(m: RotateMode) { _rotateMode.value = m; persist() }
    fun setCustomDegrees(v: String) { _customDegrees.value = v; persist() }

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
        s.putBoolean(KEY_FLIP_V, _flipVertical.value)
        s.putBoolean(KEY_FLIP_H, _flipHorizontal.value)
        s.putString(KEY_MODE, _rotateMode.value.name)
        s.putString(KEY_DEGREES, _customDegrees.value)
    }

    private fun clearAll(s: KeyValueStore) {
        s.putBoolean(KEY_FLIP_V, false)
        s.putBoolean(KEY_FLIP_H, false)
        s.putString(KEY_MODE, "")
        s.putString(KEY_DEGREES, "")
    }

    private inline fun <reified T : Enum<T>> enumOr(s: KeyValueStore?, key: String, default: T): T =
        runCatching { enumValueOf<T>(s?.getString(key, "") ?: "") }.getOrDefault(default)

    private companion object {
        const val KEY_FLIP_V = "rotate.flip_vertical"
        const val KEY_FLIP_H = "rotate.flip_horizontal"
        const val KEY_MODE = "rotate.mode"
        const val KEY_DEGREES = "rotate.degrees"
        const val KEY_REMEMBER = "rotate.remember"
    }
}

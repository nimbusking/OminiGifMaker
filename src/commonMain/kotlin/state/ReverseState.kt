package com.ominigifmaker.state

import com.ominigifmaker.model.ReverseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reverse 模块表单状态（StateHolder），支持「Remember settings」跨会话持久化。 */
class ReverseState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    private val _reverse = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_REVERSE, false) ?: false else false)
    val reverse: StateFlow<Boolean> = _reverse.asStateFlow()

    private val _boomerang = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_BOOMERANG, false) ?: false else false)
    val boomerang: StateFlow<Boolean> = _boomerang.asStateFlow()

    private val _loopCount = MutableStateFlow(if (remembered) settings?.getString(KEY_LOOP, "") ?: "" else "")
    val loopCount: StateFlow<String> = _loopCount.asStateFlow()

    private val _addTimer = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_TIMER, false) ?: false else false)
    val addTimer: StateFlow<Boolean> = _addTimer.asStateFlow()

    private val _flipVertical = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_FLIP_V, false) ?: false else false)
    val flipVertical: StateFlow<Boolean> = _flipVertical.asStateFlow()

    private val _flipHorizontal = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_FLIP_H, false) ?: false else false)
    val flipHorizontal: StateFlow<Boolean> = _flipHorizontal.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    val config: ReverseConfig
        get() = ReverseConfig(
            reverse = _reverse.value,
            boomerang = _boomerang.value,
            loopCount = _loopCount.value,
            addTimer = _addTimer.value,
            flipVertical = _flipVertical.value,
            flipHorizontal = _flipHorizontal.value,
        )

    fun setReverse(v: Boolean) { _reverse.value = v; persist() }
    fun setBoomerang(v: Boolean) { _boomerang.value = v; persist() }
    fun setLoopCount(v: String) { _loopCount.value = v; persist() }
    fun setAddTimer(v: Boolean) { _addTimer.value = v; persist() }
    fun setFlipVertical(v: Boolean) { _flipVertical.value = v; persist() }
    fun setFlipHorizontal(v: Boolean) { _flipHorizontal.value = v; persist() }

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
        s.putBoolean(KEY_REVERSE, _reverse.value)
        s.putBoolean(KEY_BOOMERANG, _boomerang.value)
        s.putString(KEY_LOOP, _loopCount.value)
        s.putBoolean(KEY_TIMER, _addTimer.value)
        s.putBoolean(KEY_FLIP_V, _flipVertical.value)
        s.putBoolean(KEY_FLIP_H, _flipHorizontal.value)
    }

    private fun clearAll(s: KeyValueStore) {
        s.putBoolean(KEY_REVERSE, false)
        s.putBoolean(KEY_BOOMERANG, false)
        s.putString(KEY_LOOP, "")
        s.putBoolean(KEY_TIMER, false)
        s.putBoolean(KEY_FLIP_V, false)
        s.putBoolean(KEY_FLIP_H, false)
    }

    private companion object {
        const val KEY_REVERSE = "reverse.reverse"
        const val KEY_BOOMERANG = "reverse.boomerang"
        const val KEY_LOOP = "reverse.loop"
        const val KEY_TIMER = "reverse.timer"
        const val KEY_FLIP_V = "reverse.flip_vertical"
        const val KEY_FLIP_H = "reverse.flip_horizontal"
        const val KEY_REMEMBER = "reverse.remember"
    }
}

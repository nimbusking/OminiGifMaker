package com.ominigifmaker.state

import com.ominigifmaker.model.SpeedConfig
import com.ominigifmaker.model.SpeedMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Speed 模块表单状态（StateHolder），支持「Remember settings」跨会话持久化。 */
class SpeedState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    private val _mode = MutableStateFlow(enumOr(settings, KEY_MODE, SpeedMode.PERCENT))
    val mode: StateFlow<SpeedMode> = _mode.asStateFlow()

    private val _value = MutableStateFlow(if (remembered) settings?.getString(KEY_VALUE, "") ?: "" else "")
    val value: StateFlow<String> = _value.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    val config: SpeedConfig get() = SpeedConfig(mode = _mode.value, value = _value.value)

    fun setMode(m: SpeedMode) { _mode.value = m; persist() }
    fun setValue(v: String) { _value.value = v; persist() }

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
        s.putString(KEY_MODE, _mode.value.name)
        s.putString(KEY_VALUE, _value.value)
    }

    private fun clearAll(s: KeyValueStore) {
        s.putString(KEY_MODE, "")
        s.putString(KEY_VALUE, "")
    }

    private inline fun <reified T : Enum<T>> enumOr(s: KeyValueStore?, key: String, default: T): T =
        runCatching { enumValueOf<T>(s?.getString(key, "") ?: "") }.getOrDefault(default)

    private companion object {
        const val KEY_MODE = "speed.mode"
        const val KEY_VALUE = "speed.value"
        const val KEY_REMEMBER = "speed.remember"
    }
}

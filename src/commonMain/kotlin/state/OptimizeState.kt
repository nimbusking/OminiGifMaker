package com.ominigifmaker.state

import com.ominigifmaker.model.OptimizeConfig
import com.ominigifmaker.model.OptimizeMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Optimize 模块表单状态（StateHolder），支持「Remember settings」跨会话持久化。 */
class OptimizeState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    private val _method = MutableStateFlow(enumOr(settings, KEY_METHOD, OptimizeMethod.LOSSY))
    val method: StateFlow<OptimizeMethod> = _method.asStateFlow()

    private val _lossyLevel = MutableStateFlow(if (remembered) settings?.getString(KEY_LOSSY, "80") ?: "80" else "80")
    val lossyLevel: StateFlow<String> = _lossyLevel.asStateFlow()

    private val _colors = MutableStateFlow(if (remembered) settings?.getString(KEY_COLORS, "256") ?: "256" else "256")
    val colors: StateFlow<String> = _colors.asStateFlow()

    private val _eliminateLocalTables = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_ELIMINATE, false) ?: false else false)
    val eliminateLocalTables: StateFlow<Boolean> = _eliminateLocalTables.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    val config: OptimizeConfig
        get() = OptimizeConfig(
            method = _method.value,
            lossyLevel = _lossyLevel.value,
            colors = _colors.value,
            eliminateLocalTables = _eliminateLocalTables.value,
        )

    fun setMethod(m: OptimizeMethod) { _method.value = m; persist() }
    fun setLossyLevel(v: String) { _lossyLevel.value = v; persist() }
    fun setColors(v: String) { _colors.value = v; persist() }
    fun setEliminateLocalTables(v: Boolean) { _eliminateLocalTables.value = v; persist() }

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
        s.putString(KEY_METHOD, _method.value.name)
        s.putString(KEY_LOSSY, _lossyLevel.value)
        s.putString(KEY_COLORS, _colors.value)
        s.putBoolean(KEY_ELIMINATE, _eliminateLocalTables.value)
    }

    private fun clearAll(s: KeyValueStore) {
        s.putString(KEY_METHOD, "")
        s.putString(KEY_LOSSY, "")
        s.putString(KEY_COLORS, "")
        s.putBoolean(KEY_ELIMINATE, false)
    }

    private inline fun <reified T : Enum<T>> enumOr(s: KeyValueStore?, key: String, default: T): T =
        runCatching { enumValueOf<T>(s?.getString(key, "") ?: "") }.getOrDefault(default)

    private companion object {
        const val KEY_METHOD = "optimize.method"
        const val KEY_LOSSY = "optimize.lossy"
        const val KEY_COLORS = "optimize.colors"
        const val KEY_ELIMINATE = "optimize.eliminate"
        const val KEY_REMEMBER = "optimize.remember"
    }
}

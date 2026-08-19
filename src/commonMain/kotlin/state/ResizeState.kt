package com.ominigifmaker.state

import com.ominigifmaker.model.ResizeConfig
import com.ominigifmaker.model.ResizeMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Resize 模块表单状态（StateHolder）。
 *
 * 支持「Remember settings」：勾选后在字段变更时写入 [KeyValueStore]，下次启动时回填。
 */
class ResizeState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    private val _width = MutableStateFlow(if (remembered) settings?.getString(KEY_WIDTH, "") ?: "" else "")
    val width: StateFlow<String> = _width.asStateFlow()

    private val _height = MutableStateFlow(if (remembered) settings?.getString(KEY_HEIGHT, "") ?: "" else "")
    val height: StateFlow<String> = _height.asStateFlow()

    private val _percentage = MutableStateFlow(if (remembered) settings?.getString(KEY_PERCENTAGE, "") ?: "" else "")
    val percentage: StateFlow<String> = _percentage.asStateFlow()

    private val _method = MutableStateFlow(
        runCatching { ResizeMethod.valueOf(settings?.getString(KEY_METHOD, "") ?: "") }
            .getOrDefault(ResizeMethod.GIFSICLE)
    )
    val method: StateFlow<ResizeMethod> = _method.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    /** 当前表单快照。 */
    val config: ResizeConfig
        get() = ResizeConfig(
            width = _width.value,
            height = _height.value,
            percentage = _percentage.value,
            method = _method.value,
        )

    fun setWidth(v: String) {
        _width.value = v
        persist()
    }

    fun setHeight(v: String) {
        _height.value = v
        persist()
    }

    fun setPercentage(v: String) {
        _percentage.value = v
        persist()
    }

    fun setMethod(m: ResizeMethod) {
        _method.value = m
        persist()
    }

    fun setRememberSettings(v: Boolean) {
        _rememberSettings.value = v
        val s = settings ?: return
        s.putBoolean(KEY_REMEMBER, v)
        if (v) persistAll(s) else clearFields(s)
    }

    private fun persist() {
        if (!_rememberSettings.value) return
        settings?.let(::persistAll)
    }

    private fun persistAll(s: KeyValueStore) {
        s.putString(KEY_WIDTH, _width.value)
        s.putString(KEY_HEIGHT, _height.value)
        s.putString(KEY_PERCENTAGE, _percentage.value)
        s.putString(KEY_METHOD, _method.value.name)
    }

    private fun clearFields(s: KeyValueStore) {
        s.putString(KEY_WIDTH, "")
        s.putString(KEY_HEIGHT, "")
        s.putString(KEY_PERCENTAGE, "")
        s.putString(KEY_METHOD, "")
    }

    private companion object {
        const val KEY_WIDTH = "resize.width"
        const val KEY_HEIGHT = "resize.height"
        const val KEY_PERCENTAGE = "resize.percentage"
        const val KEY_METHOD = "resize.method"
        const val KEY_REMEMBER = "resize.remember"
    }
}

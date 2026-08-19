package com.ominigifmaker.state

import com.ominigifmaker.model.CropAspectRatio
import com.ominigifmaker.model.CropBackground
import com.ominigifmaker.model.CropConfig
import com.ominigifmaker.model.CropMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Crop 模块表单状态（StateHolder），支持「Remember settings」跨会话持久化。 */
class CropState(private val settings: KeyValueStore? = null) {

    private val remembered = settings?.getBoolean(KEY_REMEMBER, false) ?: false

    private val _left = MutableStateFlow(if (remembered) settings?.getString(KEY_LEFT, "") ?: "" else "")
    val left: StateFlow<String> = _left.asStateFlow()

    private val _top = MutableStateFlow(if (remembered) settings?.getString(KEY_TOP, "") ?: "" else "")
    val top: StateFlow<String> = _top.asStateFlow()

    private val _width = MutableStateFlow(if (remembered) settings?.getString(KEY_WIDTH, "") ?: "" else "")
    val width: StateFlow<String> = _width.asStateFlow()

    private val _height = MutableStateFlow(if (remembered) settings?.getString(KEY_HEIGHT, "") ?: "" else "")
    val height: StateFlow<String> = _height.asStateFlow()

    private val _method = MutableStateFlow(enumOr(settings, KEY_METHOD, CropMethod.GIFSICLE))
    val method: StateFlow<CropMethod> = _method.asStateFlow()

    private val _background = MutableStateFlow(enumOr(settings, KEY_BACKGROUND, CropBackground.CHECKERED))
    val background: StateFlow<CropBackground> = _background.asStateFlow()

    private val _lockAspect = MutableStateFlow(enumOr(settings, KEY_ASPECT, CropAspectRatio.FREE))
    val lockAspect: StateFlow<CropAspectRatio> = _lockAspect.asStateFlow()

    private val _autocrop = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_AUTOCROP, false) ?: false else false)
    val autocrop: StateFlow<Boolean> = _autocrop.asStateFlow()

    private val _dontScaleLarge = MutableStateFlow(if (remembered) settings?.getBoolean(KEY_DONT_SCALE, false) ?: false else false)
    val dontScaleLarge: StateFlow<Boolean> = _dontScaleLarge.asStateFlow()

    private val _rememberSettings = MutableStateFlow(remembered)
    val rememberSettings: StateFlow<Boolean> = _rememberSettings.asStateFlow()

    /** 当前表单快照。 */
    val config: CropConfig
        get() = CropConfig(
            left = _left.value,
            top = _top.value,
            width = _width.value,
            height = _height.value,
            method = _method.value,
            background = _background.value,
            lockAspect = _lockAspect.value,
            autocrop = _autocrop.value,
            dontScaleLarge = _dontScaleLarge.value,
        )

    fun setLeft(v: String) { _left.value = v; persist() }
    fun setTop(v: String) { _top.value = v; persist() }
    fun setWidth(v: String) { _width.value = v; persist() }
    fun setHeight(v: String) { _height.value = v; persist() }
    fun setMethod(m: CropMethod) { _method.value = m; persist() }
    fun setBackground(b: CropBackground) { _background.value = b; persist() }
    fun setLockAspect(a: CropAspectRatio) { _lockAspect.value = a; persist() }
    fun setAutocrop(v: Boolean) { _autocrop.value = v; persist() }
    fun setDontScaleLarge(v: Boolean) { _dontScaleLarge.value = v; persist() }

    /** 预览拖拽联动更新裁剪矩形（图像坐标，像素）。 */
    fun setRect(l: Int, t: Int, w: Int, h: Int) {
        _left.value = l.toString()
        _top.value = t.toString()
        _width.value = w.toString()
        _height.value = h.toString()
        persist()
    }

    /** 新图像加载后重置为整图裁剪。 */
    fun resetToFull(w: Int, h: Int) {
        _left.value = "0"
        _top.value = "0"
        _width.value = w.toString()
        _height.value = h.toString()
    }

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
        s.putString(KEY_LEFT, _left.value)
        s.putString(KEY_TOP, _top.value)
        s.putString(KEY_WIDTH, _width.value)
        s.putString(KEY_HEIGHT, _height.value)
        s.putString(KEY_METHOD, _method.value.name)
        s.putString(KEY_BACKGROUND, _background.value.name)
        s.putString(KEY_ASPECT, _lockAspect.value.name)
        s.putBoolean(KEY_AUTOCROP, _autocrop.value)
        s.putBoolean(KEY_DONT_SCALE, _dontScaleLarge.value)
    }

    private fun clearAll(s: KeyValueStore) {
        s.putString(KEY_LEFT, "")
        s.putString(KEY_TOP, "")
        s.putString(KEY_WIDTH, "")
        s.putString(KEY_HEIGHT, "")
        s.putString(KEY_METHOD, "")
        s.putString(KEY_BACKGROUND, "")
        s.putString(KEY_ASPECT, "")
        s.putBoolean(KEY_AUTOCROP, false)
        s.putBoolean(KEY_DONT_SCALE, false)
    }

    private inline fun <reified T : Enum<T>> enumOr(s: KeyValueStore?, key: String, default: T): T =
        runCatching { enumValueOf<T>(s?.getString(key, "") ?: "") }.getOrDefault(default)

    private companion object {
        const val KEY_LEFT = "crop.left"
        const val KEY_TOP = "crop.top"
        const val KEY_WIDTH = "crop.width"
        const val KEY_HEIGHT = "crop.height"
        const val KEY_METHOD = "crop.method"
        const val KEY_BACKGROUND = "crop.background"
        const val KEY_ASPECT = "crop.aspect"
        const val KEY_AUTOCROP = "crop.autocrop"
        const val KEY_DONT_SCALE = "crop.dont_scale"
        const val KEY_REMEMBER = "crop.remember"
    }
}

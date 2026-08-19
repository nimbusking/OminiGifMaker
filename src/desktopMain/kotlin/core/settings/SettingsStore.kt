package com.ominigifmaker.core.settings

import java.io.File
import java.util.prefs.Preferences

/**
 * 设置持久化封装，基于 JVM 内置的 `java.util.prefs.Preferences`。
 *
 * 跨平台落地：Windows 注册表 / macOS plist / Linux 配置文件。用于「Remember settings」
 * 跨会话回填表单参数，以及引擎解压路径等运行时配置。
 */
object SettingsStore {

    private val prefs: Preferences by lazy {
        Preferences.userRoot().node("omc/ominigifmaker")
    }

    fun getString(key: String, default: String = ""): String = prefs.get(key, default)
    fun putString(key: String, value: String) = prefs.put(key, value)

    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    fun putInt(key: String, value: Int) = prefs.putInt(key, value)

    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun putBoolean(key: String, value: Boolean) = prefs.putBoolean(key, value)

    fun remove(key: String) = prefs.remove(key)
    fun clear() = prefs.clear()

    /** 引擎二进制解压目录（未设置时回落到临时目录下的固定子目录）。 */
    var engineExtractionDir: String
        get() = getString(KEY_ENGINE_DIR, defaultEngineExtractionDir())
        set(value) = putString(KEY_ENGINE_DIR, value)

    private fun defaultEngineExtractionDir(): String =
        File(System.getProperty("java.io.tmpdir"), "gif_app_engines").absolutePath

    private const val KEY_ENGINE_DIR = "engine_extraction_dir"
}

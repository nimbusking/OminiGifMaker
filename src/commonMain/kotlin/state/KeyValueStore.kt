package com.ominigifmaker.state

/**
 * 键值存储抽象，供各 StateHolder 做「Remember settings」跨会话持久化。
 *
 * desktop 端由 [com.ominigifmaker.core.settings.SettingsStore]（java.util.prefs.Preferences）实现。
 * 定义在 commonMain，使 StateHolder 不依赖 JVM 专属 API。
 */
interface KeyValueStore {
    fun getString(key: String, default: String = ""): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
}

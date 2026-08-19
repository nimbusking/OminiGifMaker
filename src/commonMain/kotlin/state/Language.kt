package com.ominigifmaker.state

/** 界面语言。 */
enum class Language(val code: String, val displayName: String) {
    ZH("zh", "中文"),
    EN("en", "English"),
    ;

    companion object {
        fun fromCode(code: String): Language? = entries.firstOrNull { it.code == code }
    }
}

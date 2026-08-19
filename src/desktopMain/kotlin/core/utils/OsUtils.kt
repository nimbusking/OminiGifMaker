package com.ominigifmaker.core.utils

/** 宿主操作系统类型，用于定位内置引擎二进制目录与可执行扩展名。 */
enum class OsType(
    val dirName: String,
    val executableExtension: String,
) {
    WINDOWS("windows", ".exe"),
    MACOS("macos", ""),
    LINUX("linux", ""),
    ;

    companion object {
        /** 基于 `os.name` 判定宿主操作系统。 */
        fun detect(): OsType {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("win") -> WINDOWS
                osName.contains("mac") || osName.contains("darwin") -> MACOS
                else -> LINUX
            }
        }
    }
}

/** 归一化 CPU 架构（基于 `os.arch`），供未来按架构分发二进制时使用。 */
fun currentArch(): String {
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
        arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64") -> "x86_64"
        else -> arch
    }
}

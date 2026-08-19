package com.ominigifmaker.core.command

import com.ominigifmaker.core.engine.EngineType

/** 一条待执行的引擎命令：目标引擎 + 参数数组。 */
data class EngineCommand(
    val engine: EngineType,
    val args: List<String>,
)

package com.ominigifmaker.ui

import androidx.compose.runtime.compositionLocalOf
import com.ominigifmaker.state.AppStrings
import com.ominigifmaker.state.Language

/** 当前语言对应的文案实例，由 Main 处根据 AppState.language 提供。 */
val LocalAppStrings = compositionLocalOf { AppStrings(Language.EN) }

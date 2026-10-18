package com.ominigifmaker.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 仅接受数字（整数）的文本输入框。[allowNegative] 为 true 时额外允许单个前导负号。 */
@Composable
fun NumberField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowNegative: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            val ok = if (allowNegative) {
                it.isEmpty() || it.matches(Regex("^-?\\d*$"))
            } else {
                it.all(Char::isDigit)
            }
            if (ok) onValueChange(it)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

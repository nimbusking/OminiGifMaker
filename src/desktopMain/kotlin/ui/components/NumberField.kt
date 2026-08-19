package com.ominigifmaker.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 仅接受数字（整数）的文本输入框。 */
@Composable
fun NumberField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all(Char::isDigit)) onValueChange(it) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

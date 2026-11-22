package com.shelf.shared.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.shelf.shared.designsystem.AppTheme

/**
 * Outlined text field of the design system.
 *
 * By default [value] is passed straight through to the underlying field, so the cursor, selection and
 * IME composing region are managed by the field exactly as with a plain Material field.
 *
 * With [cursorAtEnd] = true the component keeps a [TextFieldValue] copy so that text supplied by the
 * parent (for example a prefilled edit form) starts with the cursor at the end. The cost is that whenever
 * the parent changes the text, the selection is reset to the end and the IME composing region is dropped,
 * so only enable it for fields whose text is set programmatically rarely (prefill), not for fields that
 * transform what the user types.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    cursorAtEnd: Boolean = false
) {
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )
    val placeholderContent: (@Composable () -> Unit)? = placeholder?.let { { Text(it) } }
    val supportingContent: (@Composable () -> Unit)? = supportingText?.let { { Text(it) } }
    if (cursorAtEnd) {
        var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
        OutlinedTextField(
            value = fieldValue,
            onValueChange = {
                fieldValue = it
                if (it.text != value) onValueChange(it.text)
            },
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            placeholder = placeholderContent,
            supportingText = supportingContent,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = colors,
            shape = MaterialTheme.shapes.medium
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            placeholder = placeholderContent,
            supportingText = supportingContent,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = colors,
            shape = MaterialTheme.shapes.medium
        )
    }
}

@PreviewLightDark
@Composable
private fun TextFieldPreview() {
    AppTheme { TextField(value = "", onValueChange = {}) }
}

@PreviewLightDark
@Composable
private fun TextFieldWithValuePreview() {
    AppTheme { TextField(value = "hello@example.com", onValueChange = {}) }
}

@PreviewLightDark
@Composable
private fun TextFieldErrorPreview() {
    AppTheme {
        TextField(
            value = "bad",
            onValueChange = {},
            isError = true,
            supportingText = "Invalid email address"
        )
    }
}

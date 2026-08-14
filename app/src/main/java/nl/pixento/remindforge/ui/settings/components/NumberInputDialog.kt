package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import nl.pixento.remindforge.R

/** Longest value the field accepts, so it can't accumulate junk beyond any sane range. */
private const val MAX_DIGITS = 3

/**
 * Free numeric entry: a titled dialog with Cancel/OK where the typed value stays a draft until OK.
 * Nothing outside [range] can be committed - OK is disabled while the field is empty or out of
 * range, so the caller never has to clamp a surprise value.
 *
 * [extraContent] renders below the field, for a setting that belongs to the same picker as the
 * number rather than in a row of its own. Whatever it edits should be kept as a draft too, and
 * committed by the caller alongside the number in [onConfirm].
 */
@Composable
fun NumberInputDialog(
    title: String,
    initialValue: Int,
    range: IntRange,
    supportingText: String,
    errorText: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // Held as text, not Int: the field is legitimately empty while the user retypes a value.
    var draft by remember(initialValue) {
        val text = initialValue.toString()
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val parsed = draft.text.toIntOrNull()?.takeIf { it in range }
    val isError = draft.text.isNotEmpty() && parsed == null

    // Opening straight into an all-selected field means typing replaces rather than appends.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { new ->
                        val digits = new.text.filter { it.isDigit() }.take(MAX_DIGITS)
                        draft = new.copy(
                            text = digits,
                            selection = TextRange(
                                new.selection.start.coerceAtMost(digits.length),
                                new.selection.end.coerceAtMost(digits.length),
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    isError = isError,
                    supportingText = { Text(if (isError) errorText else supportingText) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { parsed?.let(onConfirm) }),
                )
                extraContent?.invoke(this)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null,
            ) { Text(stringResource(R.string.dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

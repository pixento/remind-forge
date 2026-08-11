package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Single-choice picker shaped like the system ringtone picker: a titled dialog, a radio list, and
 * Cancel/OK. The selection is a draft until OK is pressed, so [onPreview] can audition an option
 * (playing a vibration pattern, say) while Cancel still leaves the saved value untouched.
 */
@Composable
fun <T> RadioChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPreview: ((T) -> Unit)? = null,
) {
    var draft by remember(selected) { mutableStateOf(selected) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = draft == option,
                                onClick = {
                                    draft = option
                                    onPreview?.invoke(option)
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = draft == option, onClick = null)
                        Text(
                            text = label(option),
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

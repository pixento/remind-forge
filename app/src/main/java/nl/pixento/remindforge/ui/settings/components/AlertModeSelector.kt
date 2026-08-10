package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.pixento.remindforge.domain.model.AlertMode

@Composable
fun AlertModeSelector(
    selected: AlertMode,
    onSelect: (AlertMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        AlertMode.entries.forEach { mode ->
            Row(
                modifier = Modifier.selectable(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                ),
            ) {
                RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}

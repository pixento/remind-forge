package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.pixento.remindforge.domain.model.VibrationPatternType

@Composable
fun VibrationPatternPicker(
    selected: VibrationPatternType,
    onSelect: (VibrationPatternType) -> Unit,
    onPreview: (VibrationPatternType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        VibrationPatternType.entries.forEach { pattern ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == pattern,
                        onClick = { onSelect(pattern) },
                    ),
            ) {
                RadioButton(selected = selected == pattern, onClick = { onSelect(pattern) })
                Text(pattern.label, modifier = Modifier.weight(1f))
                TextButton(onClick = { onPreview(pattern) }) {
                    Text("Preview")
                }
            }
        }
    }
}

package nl.pixento.remindforge.ui.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import nl.pixento.remindforge.domain.model.VibrationPatternType

@Composable
fun VibrationPatternPicker(
    selected: VibrationPatternType,
    onSelect: (VibrationPatternType) -> Unit,
    onPreview: (VibrationPatternType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        title = "Vibration",
        value = selected.label,
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        RadioChoiceDialog(
            title = "Vibration pattern",
            options = VibrationPatternType.entries,
            selected = selected,
            label = { it.label },
            // Tapping a pattern buzzes it straight away, the way the system ringtone picker
            // auditions a sound - SILENT is a no-op inside the player.
            onPreview = onPreview,
            onConfirm = {
                onSelect(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

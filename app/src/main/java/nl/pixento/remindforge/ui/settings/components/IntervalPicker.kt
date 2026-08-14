package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.IntervalRandomness
import nl.pixento.remindforge.domain.model.ReminderSettings

/**
 * The interval and how much it may vary, as a single row opening a single dialog - the two belong
 * to one decision ("how often, how predictably"), so splitting them across two rows would make the
 * Schedule card read as two unrelated settings.
 */
@Composable
fun IntervalPicker(
    intervalMinutes: Int,
    randomness: IntervalRandomness,
    onIntervalChange: (Int, IntervalRandomness) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val range = ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES

    SettingsRow(
        title = stringResource(R.string.interval_title),
        value = intervalValueText(intervalMinutes, randomness),
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        // Draft, exactly like the typed number: neither is committed until OK, and both are then
        // written by the same callback so the alarm chain restarts once rather than twice.
        var randomnessDraft by remember { mutableStateOf(randomness) }

        NumberInputDialog(
            title = stringResource(R.string.interval_title),
            initialValue = intervalMinutes,
            range = range,
            supportingText = stringResource(
                R.string.interval_input_hint,
                range.first,
                range.last,
            ),
            errorText = stringResource(
                R.string.interval_input_error,
                range.first,
                range.last,
            ),
            onConfirm = {
                onIntervalChange(it, randomnessDraft)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            extraContent = {
                RandomnessOptions(
                    selected = randomnessDraft,
                    onSelect = { randomnessDraft = it },
                )
            },
        )
    }
}

/**
 * Radio rows built directly rather than out of [SettingsRow]/[SettingsGroup]: those are the grouped
 * *card* primitives, and their inset padding and surface would sit badly on a dialog.
 */
@Composable
private fun RandomnessOptions(
    selected: IntervalRandomness,
    onSelect: (IntervalRandomness) -> Unit,
) {
    Text(
        text = stringResource(R.string.interval_randomness_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = stringResource(R.string.interval_randomness_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(modifier = Modifier.selectableGroup()) {
        IntervalRandomness.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option == selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

val IntervalRandomness.label: String
    @Composable get() = stringResource(R.string.interval_randomness_value, percent)

/**
 * "Every 15 minutes" when exact, "Every 4:30 – 5:30 minutes" once a deviation is in play - one line
 * either way, so the row doesn't change shape when randomness is switched on.
 */
@Composable
private fun intervalValueText(intervalMinutes: Int, randomness: IntervalRandomness): String {
    if (randomness == IntervalRandomness.NONE) {
        return stringResource(R.string.interval_value, intervalMinutes)
    }
    val deviation = randomness.deviationSeconds(intervalMinutes)
    val total = intervalMinutes * 60L
    return stringResource(
        R.string.interval_value_range,
        formatBound(total - deviation),
        formatBound(total + deviation),
    )
}

/** Seconds are only spelled out when there are any, so whole-minute bounds stay uncluttered. */
private fun formatBound(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds == 0L) minutes.toString() else "%d:%02d".format(minutes, seconds)
}

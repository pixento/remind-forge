package nl.pixento.remindforge.ui.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.ReminderSettings

private const val INTERVAL_STEP_MINUTES = 5

@Composable
fun IntervalPicker(
    intervalMinutes: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        title = stringResource(R.string.interval_title),
        value = stringResource(R.string.interval_value, intervalMinutes),
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        RadioChoiceDialog(
            title = stringResource(R.string.interval_title),
            // Offering a fixed list instead of a stepper makes the min/max clamp structural.
            options = (
                ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES
                    step INTERVAL_STEP_MINUTES
                ).toList(),
            selected = intervalMinutes,
            label = { stringResource(R.string.interval_value, it) },
            onConfirm = {
                onIntervalChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

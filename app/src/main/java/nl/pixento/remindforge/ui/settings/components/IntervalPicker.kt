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

@Composable
fun IntervalPicker(
    intervalMinutes: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val range = ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES

    SettingsRow(
        title = stringResource(R.string.interval_title),
        value = stringResource(R.string.interval_value, intervalMinutes),
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
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
                onIntervalChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

package nl.pixento.remindforge.ui.settings.components

import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalTime

/**
 * The two ends of the daily active window as a pair of settings rows, with the divider between
 * them, so it drops straight into a [SettingsGroup]. Both open the platform time picker.
 */
@Composable
fun TimeWindowPicker(
    windowStart: LocalTime,
    windowEnd: LocalTime,
    onWindowStartChange: (LocalTime) -> Unit,
    onWindowEndChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    SettingsRow(
        title = "Start time",
        value = formatTime(windowStart),
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onWindowStartChange(LocalTime.of(hour, minute)) },
                windowStart.hour,
                windowStart.minute,
                true,
            ).show()
        },
        modifier = modifier,
    )
    SettingsDivider()
    SettingsRow(
        title = "End time",
        // An end before the start means the window wraps past midnight; say so on the value line
        // rather than in a separate note, so the row carries its own explanation.
        value = formatTime(windowEnd) + if (windowStart > windowEnd) " (next day)" else "",
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onWindowEndChange(LocalTime.of(hour, minute)) },
                windowEnd.hour,
                windowEnd.minute,
                true,
            ).show()
        },
    )
}

private fun formatTime(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

package nl.pixento.betterhabits.ui.settings.components

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.LocalTime
import nl.pixento.betterhabits.R

/**
 * The hours reminders run in - a "only during set hours" checkbox over the two time rows - as a run
 * of sibling rows that drops into the Schedule [SettingsGroup] under the interval.
 *
 * Unticking the checkbox drops the time-of-day constraint entirely, but dims the time rows rather
 * than hiding them: they keep their stored values so re-ticking restores them, and the group
 * doesn't jump around.
 *
 * The conditions that *pause* reminders live in [AutoPausePicker]; they compose with these hours
 * rather than replacing them.
 */
@Composable
fun ActiveWindowPicker(
    limitToActiveHours: Boolean,
    windowStart: LocalTime,
    windowEnd: LocalTime,
    onLimitToActiveHoursChange: (Boolean) -> Unit,
    onWindowStartChange: (LocalTime) -> Unit,
    onWindowEndChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        title = stringResource(R.string.limit_to_active_hours),
        onClick = { onLimitToActiveHoursChange(!limitToActiveHours) },
        checked = limitToActiveHours,
        trailing = { Checkbox(checked = limitToActiveHours, onCheckedChange = null) },
        modifier = modifier,
    )
    SettingsDivider()
    TimeWindowPicker(
        windowStart = windowStart,
        windowEnd = windowEnd,
        onWindowStartChange = onWindowStartChange,
        onWindowEndChange = onWindowEndChange,
        enabled = limitToActiveHours,
    )
}

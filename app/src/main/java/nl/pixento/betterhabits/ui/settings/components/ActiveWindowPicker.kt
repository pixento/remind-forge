package nl.pixento.betterhabits.ui.settings.components

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.LocalTime
import nl.pixento.betterhabits.R
import nl.pixento.betterhabits.domain.model.ActiveWindowMode

/**
 * Chooses what makes reminders active - the phone's own Do Not Disturb, or the daily times below -
 * as a run of sibling rows that drops into the Schedule [SettingsGroup] under the interval.
 *
 * The checkbox *replaces* the time window rather than adding to it, which is why ticking it dims
 * the time rows instead of leaving them live. They stay on screen (and keep their stored values, so
 * unticking restores them) because hiding them would make the group jump around, and because seeing
 * what you'd go back to is part of deciding.
 */
@Composable
fun ActiveWindowPicker(
    activeWindowMode: ActiveWindowMode,
    windowStart: LocalTime,
    windowEnd: LocalTime,
    onActiveWindowModeChange: (ActiveWindowMode) -> Unit,
    onWindowStartChange: (LocalTime) -> Unit,
    onWindowEndChange: (LocalTime) -> Unit,
    onOpenDoNotDisturbSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val followsDoNotDisturb = activeWindowMode == ActiveWindowMode.DO_NOT_DISTURB_OFF
    val toggle = {
        onActiveWindowModeChange(
            if (followsDoNotDisturb) {
                ActiveWindowMode.CUSTOM_TIMES
            } else {
                ActiveWindowMode.DO_NOT_DISTURB_OFF
            },
        )
    }

    SettingsRow(
        title = stringResource(R.string.follow_do_not_disturb_schedule),
        onClick = toggle,
        checked = followsDoNotDisturb,
        trailing = { Checkbox(checked = followsDoNotDisturb, onCheckedChange = null) },
        modifier = modifier,
    )
    SettingsDivider()
    TimeWindowPicker(
        windowStart = windowStart,
        windowEnd = windowEnd,
        onWindowStartChange = onWindowStartChange,
        onWindowEndChange = onWindowEndChange,
        enabled = !followsDoNotDisturb,
    )
    SettingsDivider()
    // Stays live whichever way the checkbox is set: it's also how you go and check what your Do Not
    // Disturb schedule actually is before deciding whether to follow it.
    SettingsRow(
        title = stringResource(R.string.do_not_disturb_settings_title),
        value = stringResource(R.string.do_not_disturb_settings_value),
        onClick = onOpenDoNotDisturbSettings,
    )
}

package nl.pixento.betterhabits.ui.settings.components

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nl.pixento.betterhabits.R

/**
 * The conditions that silence a reminder wherever it lands in the day, as a run of sibling rows for
 * its own [SettingsGroup].
 *
 * They compose with the active hours and with each other rather than replacing anything, and each
 * is judged when the alarm fires, so neither moves the next reminder - it just doesn't sound.
 * Android Auto leads so the two Do Not Disturb rows stay adjacent, the shortcut being about the
 * checkbox above it.
 *
 * Being a run of siblings, this needs the [SettingsGroup] Column around it; put it in a Box and the
 * rows stack, leaving the last one to silently swallow every click.
 */
@Composable
fun AutoPausePicker(
    pauseDuringDoNotDisturb: Boolean,
    pauseDuringAndroidAuto: Boolean,
    onPauseDuringDoNotDisturbChange: (Boolean) -> Unit,
    onPauseDuringAndroidAutoChange: (Boolean) -> Unit,
    onOpenDoNotDisturbSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        title = stringResource(R.string.pause_during_android_auto),
        onClick = { onPauseDuringAndroidAutoChange(!pauseDuringAndroidAuto) },
        checked = pauseDuringAndroidAuto,
        trailing = { Checkbox(checked = pauseDuringAndroidAuto, onCheckedChange = null) },
        modifier = modifier,
    )
    SettingsDivider()
    SettingsRow(
        title = stringResource(R.string.pause_during_do_not_disturb),
        onClick = { onPauseDuringDoNotDisturbChange(!pauseDuringDoNotDisturb) },
        checked = pauseDuringDoNotDisturb,
        trailing = { Checkbox(checked = pauseDuringDoNotDisturb, onCheckedChange = null) },
    )
    SettingsDivider()
    // Stays live whichever way the checkbox above is set: it's how you check what your Do Not
    // Disturb schedule actually is before deciding whether to follow it.
    SettingsRow(
        title = stringResource(R.string.do_not_disturb_settings_title),
        value = stringResource(R.string.do_not_disturb_settings_value),
        onClick = onOpenDoNotDisturbSettings,
    )
}

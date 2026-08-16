package nl.pixento.remindforge.ui.settings

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import nl.pixento.remindforge.R
import nl.pixento.remindforge.alerting.DoNotDisturbSettings
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import nl.pixento.remindforge.domain.model.IntervalRandomness
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.BatteryOptimization
import nl.pixento.remindforge.scheduling.ExactAlarmPermission
import nl.pixento.remindforge.ui.settings.components.ActiveWindowPicker
import nl.pixento.remindforge.ui.settings.components.BatteryOptimizationBanner
import nl.pixento.remindforge.ui.settings.components.ExactAlarmPermissionBanner
import nl.pixento.remindforge.ui.settings.components.IntervalPicker
import nl.pixento.remindforge.ui.settings.components.SettingsCardShape
import nl.pixento.remindforge.ui.settings.components.SettingsDivider
import nl.pixento.remindforge.ui.settings.components.SettingsGroup
import nl.pixento.remindforge.ui.settings.components.SettingsRow
import nl.pixento.remindforge.ui.settings.components.SettingsSectionHeader
import nl.pixento.remindforge.ui.settings.components.buildRingtonePickerIntent
import nl.pixento.remindforge.ui.settings.vibration.PickVibrationPattern
import nl.pixento.remindforge.ui.settings.vibration.label

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsScreen(
        uiState = uiState,
        onEnabledChanged = viewModel::onEnabledChanged,
        onIntervalChanged = viewModel::onIntervalChanged,
        onActiveWindowModeChanged = viewModel::onActiveWindowModeChanged,
        onWindowStartChanged = viewModel::onWindowStartChanged,
        onWindowEndChanged = viewModel::onWindowEndChanged,
        onVibrationPatternSelected = viewModel::onVibrationPatternSelected,
        onRingtoneSelected = viewModel::onRingtoneSelected,
        onOpenDoNotDisturbSettings = {
            context.startActivity(DoNotDisturbSettings.buildRequestIntent(context))
        },
        onRequestExactAlarmPermission = {
            context.startActivity(ExactAlarmPermission.buildRequestIntent(context))
        },
        onRequestBatteryOptimizationExemption = {
            context.startActivity(BatteryOptimization.buildRequestIntent(context))
        },
        onDismissBatteryOptimizationBanner = viewModel::onDismissBatteryOptimizationBanner,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEnabledChanged: (Boolean) -> Unit,
    onIntervalChanged: (Int, IntervalRandomness) -> Unit,
    onActiveWindowModeChanged: (ActiveWindowMode) -> Unit,
    onWindowStartChanged: (LocalTime) -> Unit,
    onWindowEndChanged: (LocalTime) -> Unit,
    onVibrationPatternSelected: (VibrationPatternType) -> Unit,
    onRingtoneSelected: (Uri?) -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDoNotDisturbSettings: () -> Unit = {},
    onRequestBatteryOptimizationExemption: () -> Unit = {},
    onDismissBatteryOptimizationBanner: () -> Unit = {},
) {
    // Null means the picker was left without choosing, i.e. no change.
    val vibrationLauncher = rememberLauncherForActivityResult(PickVibrationPattern) { pattern ->
        pattern?.let(onVibrationPatternSelected)
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        // Picking "Silent" returns RESULT_OK with a null URI, which is exactly how the sound
        // channel is switched off - so the result code, not the URI, is what distinguishes a real
        // choice from a cancelled picker.
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        onRingtoneSelected(
            result.data?.let {
                IntentCompat.getParcelableExtra(
                    it,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.needsExactAlarmPermission) {
            item {
                ExactAlarmPermissionBanner(onRequestPermission = onRequestExactAlarmPermission)
            }
        }

        if (uiState.showBatteryOptimizationBanner) {
            item {
                BatteryOptimizationBanner(
                    onRequestExemption = onRequestBatteryOptimizationExemption,
                    onDismiss = onDismissBatteryOptimizationBanner,
                )
            }
        }

        item {
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.settings_reminders_enabled),
                    value = enabledRowValue(uiState),
                    trailing = {
                        Switch(
                            checked = uiState.enabled,
                            onCheckedChange = onEnabledChanged,
                            modifier = Modifier.testTag("enabledSwitch"),
                        )
                    },
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_schedule_title),
                    description = stringResource(R.string.settings_schedule_description),
                )
                SettingsGroup {
                    IntervalPicker(
                        intervalMinutes = uiState.intervalMinutes,
                        randomness = uiState.intervalRandomness,
                        onIntervalChange = onIntervalChanged,
                    )
                    SettingsDivider()
                    ActiveWindowPicker(
                        activeWindowMode = uiState.activeWindowMode,
                        windowStart = uiState.windowStart,
                        windowEnd = uiState.windowEnd,
                        onActiveWindowModeChange = onActiveWindowModeChanged,
                        onWindowStartChange = onWindowStartChanged,
                        onWindowEndChange = onWindowEndChanged,
                        onOpenDoNotDisturbSettings = onOpenDoNotDisturbSettings,
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_alerts_title),
                    description = stringResource(R.string.settings_alerts_description),
                )
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.vibration_label),
                        value = uiState.vibrationPattern.label,
                        onClick = { vibrationLauncher.launch(uiState.vibrationPattern) },
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.sound_label),
                        value = uiState.ringtoneTitle ?: stringResource(R.string.silent_label),
                        onClick = {
                            ringtoneLauncher.launch(
                                buildRingtonePickerIntent(uiState.ringtoneUri?.let(Uri::parse)),
                            )
                        },
                    )
                }
            }
        }

        if (uiState.hasNoAlertSelected) {
            item { NoAlertSelectedWarning() }
        }
    }
}

@Composable
private fun NoAlertSelectedWarning(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.no_alert_warning))
        }
    }
}

/**
 * Either when the next reminder lands or why it won't alert - this one line is where the chain's
 * state is reported, rather than a countdown here and a contradicting notice elsewhere on the
 * screen.
 *
 * The time is the instant the pending alarm will actually fire, as recorded by whoever scheduled it
 * - not a fresh now + interval estimate, which would silently disagree with the running chain (and
 * appear to push the next reminder forward) every time this screen was opened. While Do Not Disturb
 * pauses the alerts that instant is still real, but promising a reminder at it would be a lie, so
 * the paused state wins.
 */
@Composable
private fun enabledRowValue(uiState: SettingsUiState): String? {
    val zone = remember { ZoneId.systemDefault() }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    if (!uiState.enabled) return null
    if (uiState.remindersPausedByDoNotDisturb) {
        return stringResource(R.string.paused_for_do_not_disturb)
    }
    val next = uiState.nextTriggerAtMillis ?: return null
    return stringResource(
        R.string.next_reminder_around,
        formatter.format(Instant.ofEpochMilli(next).atZone(zone)),
    )
}

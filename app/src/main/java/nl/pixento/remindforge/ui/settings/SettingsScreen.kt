package nl.pixento.remindforge.ui.settings

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import nl.pixento.remindforge.domain.NextTriggerCalculator
import nl.pixento.remindforge.domain.model.AlertMode
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.BatteryOptimization
import nl.pixento.remindforge.scheduling.ExactAlarmPermission
import nl.pixento.remindforge.ui.settings.components.AlertModeSelector
import nl.pixento.remindforge.ui.settings.components.BatteryOptimizationBanner
import nl.pixento.remindforge.ui.settings.components.ExactAlarmPermissionBanner
import nl.pixento.remindforge.ui.settings.components.IntervalStepper
import nl.pixento.remindforge.ui.settings.components.TimeWindowPicker
import nl.pixento.remindforge.ui.settings.components.VibrationPatternPicker
import nl.pixento.remindforge.ui.settings.components.buildRingtonePickerIntent

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsScreen(
        uiState = uiState,
        onEnabledChanged = viewModel::onEnabledChanged,
        onIntervalChanged = viewModel::onIntervalChanged,
        onWindowStartChanged = viewModel::onWindowStartChanged,
        onWindowEndChanged = viewModel::onWindowEndChanged,
        onAlertModeChanged = viewModel::onAlertModeChanged,
        onVibrationPatternSelected = viewModel::onVibrationPatternSelected,
        onPreviewVibration = viewModel::onPreviewVibration,
        onRingtoneSelected = viewModel::onRingtoneSelected,
        onPreviewRingtone = viewModel::onPreviewRingtone,
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
    onIntervalChanged: (Int) -> Unit,
    onWindowStartChanged: (LocalTime) -> Unit,
    onWindowEndChanged: (LocalTime) -> Unit,
    onAlertModeChanged: (AlertMode) -> Unit,
    onVibrationPatternSelected: (VibrationPatternType) -> Unit,
    onPreviewVibration: (VibrationPatternType) -> Unit,
    onRingtoneSelected: (Uri) -> Unit,
    onPreviewRingtone: (Uri) -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestBatteryOptimizationExemption: () -> Unit = {},
    onDismissBatteryOptimizationBanner: () -> Unit = {},
) {
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.let {
            IntentCompat.getParcelableExtra(
                it,
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
        }
        if (uri != null) onRingtoneSelected(uri)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Enabled", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = uiState.enabled,
                    onCheckedChange = onEnabledChanged,
                    modifier = Modifier.testTag("enabledSwitch"),
                )
            }
        }

        if (uiState.enabled) {
            item {
                NextReminderEstimate(uiState)
            }
        }

        item {
            Column {
                Text("Interval", style = MaterialTheme.typography.titleMedium)
                IntervalStepper(uiState.intervalMinutes, onIntervalChanged)
            }
        }

        item {
            Column {
                Text("Active window", style = MaterialTheme.typography.titleMedium)
                TimeWindowPicker(
                    uiState.windowStart,
                    uiState.windowEnd,
                    onWindowStartChanged,
                    onWindowEndChanged
                )
            }
        }

        item {
            Column {
                Text("Alert", style = MaterialTheme.typography.titleMedium)
                AlertModeSelector(uiState.alertMode, onAlertModeChanged)
            }
        }

        if (uiState.alertMode == AlertMode.VIBRATION) {
            item {
                VibrationPatternPicker(
                    selected = uiState.vibrationPattern,
                    onSelect = onVibrationPatternSelected,
                    onPreview = onPreviewVibration,
                )
            }
        } else {
            item {
                Column {
                    Text(uiState.ringtoneTitle ?: "No ringtone selected")
                    Row {
                        TextButton(onClick = {
                            val currentUri = uiState.ringtoneUri?.let(Uri::parse)
                            ringtoneLauncher.launch(buildRingtonePickerIntent(currentUri))
                        }) {
                            Text("Choose ringtone")
                        }
                        val ringtoneUri = uiState.ringtoneUri
                        if (ringtoneUri != null) {
                            TextButton(onClick = { onPreviewRingtone(Uri.parse(ringtoneUri)) }) {
                                Text("Preview")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live estimate of the next reminder time, computed from current settings - not the actual
 * persisted alarm-chain instant (AlarmManager has no reliable cross-process query for that).
 * Recomputed on every relevant settings change AND periodically as wall-clock time passes,
 * so the estimate doesn't go stale (and drift into the past) while the screen sits idle.
 */
@Composable
private fun NextReminderEstimate(uiState: SettingsUiState, modifier: Modifier = Modifier) {
    val zone = remember { ZoneId.systemDefault() }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Instant.now()
        }
    }
    val next = remember(uiState.intervalMinutes, uiState.windowStart, uiState.windowEnd, now) {
        NextTriggerCalculator.nextTrigger(
            referenceInstant = now,
            zone = zone,
            intervalMinutes = uiState.intervalMinutes,
            windowStart = uiState.windowStart,
            windowEnd = uiState.windowEnd,
        )
    }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    Text(
        "Next reminder around ${formatter.format(next.atZone(zone))}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

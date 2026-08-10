package nl.pixento.remindforge.ui.settings

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nl.pixento.remindforge.alerting.AlertPlayer
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.ReminderScheduleCoordinator
import nl.pixento.remindforge.domain.model.AlertMode
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.BatteryOptimization
import nl.pixento.remindforge.scheduling.ExactAlarmPermission

class SettingsViewModel(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val scheduleCoordinator: ReminderScheduleCoordinator,
    private val alertPlayer: AlertPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var batteryBannerDismissed = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(
                    enabled = settings.enabled,
                    intervalMinutes = settings.intervalMinutes,
                    windowStart = settings.windowStart,
                    windowEnd = settings.windowEnd,
                    alertMode = settings.alertMode,
                    vibrationPattern = settings.vibrationPattern,
                    ringtoneUri = settings.ringtoneUri,
                    ringtoneTitle = resolveRingtoneTitle(settings.ringtoneUri),
                )
            }
        }
        refreshPermissionState()
        viewModelScope.launch {
            scheduleCoordinator.healIfNeeded()
        }
    }

    fun onEnabledChanged(enabled: Boolean) = persist { setEnabled(enabled) }

    fun onIntervalChanged(minutes: Int) = persist { setIntervalMinutes(minutes) }

    fun onWindowStartChanged(time: LocalTime) = persist { setWindowStart(time) }

    fun onWindowEndChanged(time: LocalTime) = persist { setWindowEnd(time) }

    fun onAlertModeChanged(mode: AlertMode) = persist { setAlertMode(mode) }

    fun onVibrationPatternSelected(pattern: VibrationPatternType) =
        persist { setVibrationPattern(pattern) }

    fun onRingtoneSelected(uri: Uri) = persist { setRingtoneUri(uri.toString()) }

    fun onPreviewVibration(pattern: VibrationPatternType) {
        alertPlayer.playVibration(pattern)
    }

    fun onPreviewRingtone(uri: Uri) {
        alertPlayer.playRingtone(uri.toString())
    }

    fun onStopPreview() {
        alertPlayer.stopPreview()
    }

    fun onDismissBatteryOptimizationBanner() {
        batteryBannerDismissed = true
        _uiState.value = _uiState.value.copy(showBatteryOptimizationBanner = false)
    }

    /** Call from Activity.onResume(): permission grants happen in a system settings screen. */
    fun onExactAlarmPermissionResumeCheck() {
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        _uiState.value = _uiState.value.copy(
            needsExactAlarmPermission = !ExactAlarmPermission.canScheduleExactAlarms(appContext),
            showBatteryOptimizationBanner = !batteryBannerDismissed &&
                    !BatteryOptimization.isIgnoringBatteryOptimizations(appContext),
        )
    }

    private fun resolveRingtoneTitle(uriString: String?): String? {
        if (uriString == null) return null
        return try {
            RingtoneManager.getRingtone(appContext, Uri.parse(uriString))?.getTitle(appContext)
        } catch (e: SecurityException) {
            null
        }
    }

    private fun persist(write: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch {
            settingsRepository.write()
            scheduleCoordinator.rescheduleFromNow()
        }
    }
}

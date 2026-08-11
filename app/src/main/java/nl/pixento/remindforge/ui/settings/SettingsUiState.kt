package nl.pixento.remindforge.ui.settings

import java.time.LocalTime
import nl.pixento.remindforge.domain.model.VibrationPatternType

data class SettingsUiState(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 15,
    val windowStart: LocalTime = LocalTime.of(9, 0),
    val windowEnd: LocalTime = LocalTime.of(17, 0),
    val vibrationPattern: VibrationPatternType = VibrationPatternType.SHORT_PULSE,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
    /** Epoch millis of the alarm actually pending in the chain; null when nothing is scheduled. */
    val nextTriggerAtMillis: Long? = null,
    val needsExactAlarmPermission: Boolean = false,
    val showBatteryOptimizationBanner: Boolean = false,
) {
    val isOvernightWrap: Boolean get() = windowStart > windowEnd

    /** Both channels silenced: reminders would still fire, just with nothing to notice. */
    val hasNoAlertSelected: Boolean
        get() = vibrationPattern == VibrationPatternType.SILENT && ringtoneUri == null
}

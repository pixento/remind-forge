package nl.pixento.remindforge.ui.settings

import java.time.LocalTime
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import nl.pixento.remindforge.domain.model.VibrationPatternType

data class SettingsUiState(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 15,
    val activeWindowMode: ActiveWindowMode = ActiveWindowMode.CUSTOM_TIMES,
    val windowStart: LocalTime = LocalTime.of(9, 0),
    val windowEnd: LocalTime = LocalTime.of(17, 0),
    val vibrationPattern: VibrationPatternType = VibrationPatternType.SHORT_PULSE,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
    /** Epoch millis of the alarm actually pending in the chain; null when nothing is scheduled. */
    val nextTriggerAtMillis: Long? = null,
    val needsExactAlarmPermission: Boolean = false,
    val showBatteryOptimizationBanner: Boolean = false,
    /** Whether the phone is in Do Not Disturb right now, as of the last device-state refresh. */
    val doNotDisturbActive: Boolean = false,
) {
    /** Both channels silenced: reminders would still fire, just with nothing to notice. */
    val hasNoAlertSelected: Boolean
        get() = vibrationPattern == VibrationPatternType.SILENT && ringtoneUri == null

    /**
     * The chain is still ticking and the next-reminder line still counts down, but every tick will
     * skip its alert until DND ends - so the screen has to say so rather than promise a reminder.
     */
    val remindersPausedByDoNotDisturb: Boolean
        get() = enabled &&
                activeWindowMode == ActiveWindowMode.DO_NOT_DISTURB_OFF &&
                doNotDisturbActive
}

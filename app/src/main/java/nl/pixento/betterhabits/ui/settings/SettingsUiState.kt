package nl.pixento.betterhabits.ui.settings

import java.time.LocalTime
import nl.pixento.betterhabits.domain.model.IntervalRandomness
import nl.pixento.betterhabits.domain.model.VibrationPatternType

data class SettingsUiState(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 15,
    val intervalRandomness: IntervalRandomness = IntervalRandomness.NONE,
    val limitToActiveHours: Boolean = true,
    val windowStart: LocalTime = LocalTime.of(9, 0),
    val windowEnd: LocalTime = LocalTime.of(17, 0),
    val pauseDuringDoNotDisturb: Boolean = false,
    val pauseDuringAndroidAuto: Boolean = false,
    val vibrationPattern: VibrationPatternType = VibrationPatternType.SHORT_PULSE,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
    /** Epoch millis of the alarm actually pending in the chain; null when nothing is scheduled. */
    val nextTriggerAtMillis: Long? = null,
    val needsExactAlarmPermission: Boolean = false,
    val showBatteryOptimizationBanner: Boolean = false,
    /** Whether the phone is in Do Not Disturb right now, as of the last device-state refresh. */
    val doNotDisturbActive: Boolean = false,
    /** Whether a car is connected right now, as of the last device-state refresh. */
    val androidAutoConnected: Boolean = false,
) {
    /** Both channels silenced: reminders would still fire, just with nothing to notice. */
    val hasNoAlertSelected: Boolean
        get() = vibrationPattern == VibrationPatternType.SILENT && ringtoneUri == null

    /**
     * The chain is still ticking and its pending alarm is still real, but every tick will skip its
     * alert until DND ends - so the enabled row says this rather than promising a reminder at a
     * time nothing will happen at.
     */
    val remindersPausedByDoNotDisturb: Boolean
        get() = enabled && pauseDuringDoNotDisturb && doNotDisturbActive

    /** The same, for a connected car. */
    val remindersPausedByAndroidAuto: Boolean
        get() = enabled && pauseDuringAndroidAuto && androidAutoConnected
}

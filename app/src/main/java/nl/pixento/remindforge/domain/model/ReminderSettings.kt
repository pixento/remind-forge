package nl.pixento.remindforge.domain.model

import java.time.LocalTime

data class ReminderSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 15,
    val windowStart: LocalTime = LocalTime.of(9, 0),
    val windowEnd: LocalTime = LocalTime.of(17, 0),
    /** [VibrationPatternType.SILENT] silences the vibration channel. */
    val vibrationPattern: VibrationPatternType = VibrationPatternType.SHORT_PULSE,
    /** `null` silences the sound channel; the two channels are independent and can both play. */
    val ringtoneUri: String? = null,
) {
    companion object {
        const val MIN_INTERVAL_MINUTES = 5
        const val MAX_INTERVAL_MINUTES = 30
    }
}

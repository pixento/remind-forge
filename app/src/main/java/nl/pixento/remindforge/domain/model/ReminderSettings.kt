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
    /**
     * Whether [other] would produce the same alarm chain as this. Only these four fields feed
     * [nl.pixento.remindforge.domain.NextTriggerCalculator]; the alert channels are read fresh on
     * every tick, so changing them must not restart the chain (which would push the next reminder a
     * full interval away).
     */
    fun schedulesSameAs(other: ReminderSettings): Boolean =
        enabled == other.enabled &&
                intervalMinutes == other.intervalMinutes &&
                windowStart == other.windowStart &&
                windowEnd == other.windowEnd

    companion object {
        const val MIN_INTERVAL_MINUTES = 2
        const val MAX_INTERVAL_MINUTES = 120
    }
}

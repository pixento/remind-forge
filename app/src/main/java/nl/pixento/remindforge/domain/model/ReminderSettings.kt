package nl.pixento.remindforge.domain.model

import java.time.LocalTime

data class ReminderSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 15,
    val activeWindowMode: ActiveWindowMode = ActiveWindowMode.CUSTOM_TIMES,
    val windowStart: LocalTime = LocalTime.of(9, 0),
    val windowEnd: LocalTime = LocalTime.of(17, 0),
    /** [VibrationPatternType.SILENT] silences the vibration channel. */
    val vibrationPattern: VibrationPatternType = VibrationPatternType.SHORT_PULSE,
    /** `null` silences the sound channel; the two channels are independent and can both play. */
    val ringtoneUri: String? = null,
) {
    /**
     * The time-of-day constraint on future trigger slots, or `null` if there isn't one.
     *
     * Null in [ActiveWindowMode.DO_NOT_DISTURB_OFF]: whether DND is on can only be read for *now*,
     * never predicted for a future instant, so that mode can't clamp the next trigger into a window
     * and is instead judged tick by tick in
     * [nl.pixento.remindforge.domain.TriggerReminderUseCase].
     */
    val activeWindow: DailyWindow?
        get() = when (activeWindowMode) {
            ActiveWindowMode.CUSTOM_TIMES -> DailyWindow(windowStart, windowEnd)
            ActiveWindowMode.DO_NOT_DISTURB_OFF -> null
        }

    /**
     * Whether [other] would produce the same alarm chain as this. Only these fields feed
     * [nl.pixento.remindforge.domain.NextTriggerCalculator]; the alert channels are read fresh on
     * every tick, so changing them must not restart the chain (which would push the next reminder a
     * full interval away).
     *
     * The window times stay in here even though they're unused in
     * [ActiveWindowMode.DO_NOT_DISTURB_OFF]: comparing them unconditionally is simpler than
     * reasoning about which mode was in effect before *and* after the write.
     */
    fun schedulesSameAs(other: ReminderSettings): Boolean =
        enabled == other.enabled &&
                intervalMinutes == other.intervalMinutes &&
                activeWindowMode == other.activeWindowMode &&
                windowStart == other.windowStart &&
                windowEnd == other.windowEnd

    companion object {
        const val MIN_INTERVAL_MINUTES = 2
        const val MAX_INTERVAL_MINUTES = 120
    }
}

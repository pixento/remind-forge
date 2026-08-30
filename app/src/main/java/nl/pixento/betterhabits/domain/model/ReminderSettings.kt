package nl.pixento.betterhabits.domain.model

import java.time.LocalTime

data class ReminderSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 15,
    /** How far each gap may stray from [intervalMinutes]; [IntervalRandomness.NONE] is exact. */
    val intervalRandomness: IntervalRandomness = IntervalRandomness.NONE,
    /** Whether [windowStart]..[windowEnd] applies at all; `false` means "no fixed hours". */
    val limitToActiveHours: Boolean = true,
    val windowStart: LocalTime = LocalTime.of(9, 0),
    val windowEnd: LocalTime = LocalTime.of(17, 0),
    /**
     * Skip the alert while the phone is in Do Not Disturb. Off by default because
     * [nl.pixento.betterhabits.alerting.AndroidAlertPlayer] classes alerts as `USAGE_ALARM`
     * precisely so they punch *through* it.
     */
    val pauseDuringDoNotDisturb: Boolean = false,
    /** Skip the alert while the phone is connected to a car (Android Auto projection). */
    val pauseDuringAndroidAuto: Boolean = false,
    /** [VibrationPatternType.SILENT] silences the vibration channel. */
    val vibrationPattern: VibrationPatternType = VibrationPatternType.SHORT_PULSE,
    /** `null` silences the sound channel; the two channels are independent and can both play. */
    val ringtoneUri: String? = null,
) {
    /**
     * The time-of-day constraint on future trigger slots, or `null` if there isn't one.
     *
     * Only the hours can be baked into a future trigger time: Do Not Disturb and a car connection
     * are readable for *now* alone, so they never clamp the next trigger and are judged tick by
     * tick in [nl.pixento.betterhabits.domain.TriggerReminderUseCase] instead.
     */
    val activeWindow: DailyWindow?
        get() = if (limitToActiveHours) DailyWindow(windowStart, windowEnd) else null

    /**
     * Whether [other] would produce the same alarm chain as this. Only these fields feed
     * [nl.pixento.betterhabits.domain.NextTriggerCalculator]; everything else is read fresh on every
     * tick, so changing it must not restart the chain (which would push the next reminder a full
     * interval away).
     *
     * So [pauseDuringDoNotDisturb] and [pauseDuringAndroidAuto] are absent - they decide whether an
     * already-scheduled tick alerts, never when it lands - while [limitToActiveHours] is here
     * because it changes [activeWindow].
     *
     * The window times are compared even while [limitToActiveHours] is off, which is simpler than
     * reasoning about which way the flag was set before *and* after the write.
     */
    fun schedulesSameAs(other: ReminderSettings): Boolean =
        enabled == other.enabled &&
                intervalMinutes == other.intervalMinutes &&
                intervalRandomness == other.intervalRandomness &&
                limitToActiveHours == other.limitToActiveHours &&
                windowStart == other.windowStart &&
                windowEnd == other.windowEnd

    companion object {
        const val MIN_INTERVAL_MINUTES = 2
        const val MAX_INTERVAL_MINUTES = 120
    }
}

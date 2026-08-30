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
     * Skip the alert while the phone is in Do Not Disturb.
     *
     * Off by default on purpose: [nl.pixento.betterhabits.alerting.AndroidAlertPlayer] classes
     * alerts as `USAGE_ALARM` precisely so they punch *through* Do Not Disturb, and defaulting this
     * on would silently reverse that decision for everyone.
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
     * Only the hours can be baked into a future trigger time. The two pause conditions can't:
     * whether Do Not Disturb is on, or a car is connected, can only be read for *now*, never
     * predicted for a future instant - so they never clamp the next trigger and are instead judged
     * tick by tick in [nl.pixento.betterhabits.domain.TriggerReminderUseCase].
     */
    val activeWindow: DailyWindow?
        get() = if (limitToActiveHours) DailyWindow(windowStart, windowEnd) else null

    /**
     * Whether [other] would produce the same alarm chain as this. Only these fields feed
     * [nl.pixento.betterhabits.domain.NextTriggerCalculator]; everything else is read fresh on every
     * tick, so changing it must not restart the chain (which would push the next reminder a full
     * interval away).
     *
     * That is why [pauseDuringDoNotDisturb] and [pauseDuringAndroidAuto] are deliberately absent:
     * they only ever decide whether an already-scheduled tick alerts, never when it lands.
     * [limitToActiveHours] *is* here, because it changes [activeWindow].
     *
     * The window times stay in here even though they're unused while [limitToActiveHours] is off:
     * comparing them unconditionally is simpler than reasoning about which way the flag was set
     * before *and* after the write.
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

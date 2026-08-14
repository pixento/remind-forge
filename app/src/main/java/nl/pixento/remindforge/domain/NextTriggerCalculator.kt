package nl.pixento.remindforge.domain

import nl.pixento.remindforge.domain.model.DailyWindow
import nl.pixento.remindforge.domain.model.IntervalRandomness
import nl.pixento.remindforge.domain.model.ReminderSettings
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * Pure, framework-free trigger-time math. Given the previously *scheduled* instant (not the
 * actual fire time, to avoid compounding drift across a long alarm chain), computes the next
 * instant the reminder should fire, clamped into the active daily window.
 */
object NextTriggerCalculator {

    /**
     * @param referenceInstant the prior scheduled trigger, or now() when computing fresh
     *   (e.g. on enable or settings change).
     * @param window the daily window to clamp into, or `null` for no time-of-day constraint at all
     *   - which is the Do-Not-Disturb-following mode, where whether a slot is active can only be
     *   decided when it fires, not now.
     * @param now the current instant, used only to skip slots that are already in the past when
     *   the firing alarm was itself delivered late. Defaults to [referenceInstant], which is
     *   correct for the compute-fresh case where the reference already *is* now.
     * @param randomness how far this gap may stray from [intervalMinutes], making the reminder
     *   harder to anticipate. Since each tick keys off the previously *scheduled* instant, the
     *   offsets accumulate and the cadence random-walks over the day - which is the point. The
     *   long-run average stays at [intervalMinutes], and a [window] re-anchors the chain to its
     *   start every day.
     * @param random the source of that deviation; injectable so tests can seed it.
     */
    fun nextTrigger(
        referenceInstant: Instant,
        zone: ZoneId,
        intervalMinutes: Int,
        window: DailyWindow?,
        now: Instant = referenceInstant,
        randomness: IntervalRandomness = IntervalRandomness.NONE,
        random: Random = Random.Default,
    ): Instant {
        val supported = ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES
        require(intervalMinutes in supported) {
            "intervalMinutes must be between ${supported.first} and ${supported.last}, " +
                    "was $intervalMinutes"
        }

        val naive = firstSlotAfter(
            referenceInstant,
            jitteredInterval(intervalMinutes, randomness, random),
            now,
        )
        if (window == null) return naive

        val naiveZoned = ZonedDateTime.ofInstant(naive, zone)
        return if (window.contains(naiveZoned.toLocalTime())) {
            naive
        } else {
            nextWindowStart(naiveZoned, window.start)
        }
    }

    /**
     * Defensive re-check usable at alarm-fire time (e.g. after a clock change). A `null` [window]
     * imposes no constraint, so every instant is within it.
     */
    fun isWithinWindow(instant: Instant, zone: ZoneId, window: DailyWindow?): Boolean {
        if (window == null) return true
        return window.contains(ZonedDateTime.ofInstant(instant, zone).toLocalTime())
    }

    /**
     * The configured interval displaced by a uniform draw from `[-deviation, +deviation]` seconds.
     *
     * The deviation is applied to the *interval* rather than to the slot [firstSlotAfter] returns,
     * because that function's whole job is to guarantee a result strictly after `now`; nudging its
     * answer afterwards could push it back into the past. Jittering the input keeps that guarantee -
     * and the catch-up behaviour below - intact for free.
     */
    private fun jitteredInterval(
        intervalMinutes: Int,
        randomness: IntervalRandomness,
        random: Random,
    ): Duration {
        val base = Duration.ofMinutes(intervalMinutes.toLong())
        val deviation = randomness.deviationSeconds(intervalMinutes)
        if (deviation == 0L) return base
        // nextLong's upper bound is exclusive, so +1 to make +deviation itself reachable.
        return base.plusSeconds(random.nextLong(-deviation, deviation + 1))
    }

    /**
     * The earliest `reference + k * interval` (k >= 1) that is still in the future.
     *
     * Doze can hold an exact alarm well past its scheduled time - deferrals of several minutes are
     * routine on an idle device. Since the chain keys off the *scheduled* time, the naive next slot
     * can then already be in the past, and AlarmManager fires a past-dated alarm immediately: the
     * chain replays every missed tick back to back, buzzing repeatedly within seconds. Skipping
     * whole intervals rather than restarting from [now] keeps the original cadence (a 15-minute
     * chain started at :02 still lands on :17, :32, ...) while never scheduling into the past.
     */
    private fun firstSlotAfter(reference: Instant, interval: Duration, now: Instant): Instant {
        val naive = reference.plus(interval)
        if (naive.isAfter(now)) return naive

        val missed = Duration.between(reference, now).toMillis() / interval.toMillis()
        return reference.plus(interval.multipliedBy(missed + 1))
    }

    private fun nextWindowStart(from: ZonedDateTime, windowStart: LocalTime): Instant {
        val todayStart = from.toLocalDate().atTime(windowStart).atZone(from.zone)
        val candidate = if (!todayStart.isBefore(from)) todayStart else todayStart.plusDays(1)
        return candidate.toInstant()
    }
}

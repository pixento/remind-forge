package nl.pixento.remindforge.domain

import nl.pixento.remindforge.domain.model.ReminderSettings
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure, framework-free trigger-time math. Given the previously *scheduled* instant (not the
 * actual fire time, to avoid compounding drift across a long alarm chain), computes the next
 * instant the reminder should fire, clamped into the active daily window.
 */
object NextTriggerCalculator {

    /**
     * @param referenceInstant the prior scheduled trigger, or now() when computing fresh
     *   (e.g. on enable or settings change).
     * @param windowEnd if before [windowStart], the window is treated as wrapping past midnight
     *   (e.g. 22:00-06:00). If equal to [windowStart], the window is treated as always-active.
     * @param now the current instant, used only to skip slots that are already in the past when
     *   the firing alarm was itself delivered late. Defaults to [referenceInstant], which is
     *   correct for the compute-fresh case where the reference already *is* now.
     */
    fun nextTrigger(
        referenceInstant: Instant,
        zone: ZoneId,
        intervalMinutes: Int,
        windowStart: LocalTime,
        windowEnd: LocalTime,
        now: Instant = referenceInstant,
    ): Instant {
        val supported = ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES
        require(intervalMinutes in supported) {
            "intervalMinutes must be between ${supported.first} and ${supported.last}, " +
                    "was $intervalMinutes"
        }

        val naive = firstSlotAfter(
            referenceInstant,
            Duration.ofMinutes(intervalMinutes.toLong()),
            now,
        )
        val naiveZoned = ZonedDateTime.ofInstant(naive, zone)

        return if (isWithinWindow(naiveZoned.toLocalTime(), windowStart, windowEnd)) {
            naive
        } else {
            nextWindowStart(naiveZoned, windowStart)
        }
    }

    /** Defensive re-check usable at alarm-fire time (e.g. after a clock change). */
    fun isWithinWindow(
        instant: Instant,
        zone: ZoneId,
        windowStart: LocalTime,
        windowEnd: LocalTime
    ): Boolean {
        val time = ZonedDateTime.ofInstant(instant, zone).toLocalTime()
        return isWithinWindow(time, windowStart, windowEnd)
    }

    private fun isWithinWindow(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        if (start == end) return true
        return if (start < end) {
            time >= start && time < end
        } else {
            // Overnight wrap: active from `start` through midnight to `end`.
            time >= start || time < end
        }
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

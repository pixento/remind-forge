package nl.pixento.remindforge.domain

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
     */
    fun nextTrigger(
        referenceInstant: Instant,
        zone: ZoneId,
        intervalMinutes: Int,
        windowStart: LocalTime,
        windowEnd: LocalTime,
    ): Instant {
        require(intervalMinutes in 5..30) {
            "intervalMinutes must be between 5 and 30, was $intervalMinutes"
        }

        val naive = referenceInstant.plus(Duration.ofMinutes(intervalMinutes.toLong()))
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

    private fun nextWindowStart(from: ZonedDateTime, windowStart: LocalTime): Instant {
        val todayStart = from.toLocalDate().atTime(windowStart).atZone(from.zone)
        val candidate = if (!todayStart.isBefore(from)) todayStart else todayStart.plusDays(1)
        return candidate.toInstant()
    }
}

package nl.pixento.betterhabits.domain.model

import java.time.LocalTime

/**
 * A daily recurring time-of-day window, the same one every day.
 *
 * @param end if before [start], the window wraps past midnight (e.g. 22:00-06:00). If equal to
 *   [start], the window is always active.
 */
data class DailyWindow(
    val start: LocalTime,
    val end: LocalTime,
) {
    /** Start inclusive, end exclusive, so back-to-back windows don't overlap on their boundary. */
    fun contains(time: LocalTime): Boolean {
        if (start == end) return true
        return if (start < end) {
            time >= start && time < end
        } else {
            // Overnight wrap: active from `start` through midnight to `end`.
            time >= start || time < end
        }
    }
}

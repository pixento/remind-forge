package nl.pixento.remindforge.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NextTriggerCalculatorTest {

    private val utc = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 1, 1)
    private val nextDay = day.plusDays(1)

    private fun instantAt(date: LocalDate, time: LocalTime, zone: ZoneId = utc) =
        LocalDateTime.of(date, time).atZone(zone).toInstant()

    @Test
    fun `within window, no boundary, returns naive tick unchanged`() {
        val reference = instantAt(day, LocalTime.of(10, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(10, 15)), result)
    }

    @Test
    fun `naive equal to window start is included (inclusive start)`() {
        val reference = instantAt(day, LocalTime.of(8, 45))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(9, 0)), result)
    }

    @Test
    fun `naive equal to window end is excluded, rolls to next day start`() {
        val reference = instantAt(day, LocalTime.of(16, 45))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        assertEquals(instantAt(nextDay, LocalTime.of(9, 0)), result)
    }

    @Test
    fun `non-wrap window, naive before start jumps to today's start`() {
        val reference = instantAt(day, LocalTime.of(5, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 30,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(9, 0)), result)
    }

    @Test
    fun `non-wrap window, naive after end jumps to tomorrow's start`() {
        val reference = instantAt(day, LocalTime.of(20, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 10,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        assertEquals(instantAt(nextDay, LocalTime.of(9, 0)), result)
    }

    @Test
    fun `overnight wrap window, naive inside wrapped range is included`() {
        val reference = instantAt(day, LocalTime.of(23, 15))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(22, 0),
            windowEnd = LocalTime.of(6, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(23, 30)), result)
    }

    @Test
    fun `overnight wrap window, naive in the daytime gap jumps to today's start`() {
        val reference = instantAt(day, LocalTime.of(6, 45))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(22, 0),
            windowEnd = LocalTime.of(6, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(22, 0)), result)
    }

    @Test
    fun `overnight wrap window, naive equal to end is excluded`() {
        val reference = instantAt(day, LocalTime.of(5, 45))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(22, 0),
            windowEnd = LocalTime.of(6, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(22, 0)), result)
    }

    @Test
    fun `overnight wrap window, naive equal to start is included`() {
        val reference = instantAt(day, LocalTime.of(21, 45))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(22, 0),
            windowEnd = LocalTime.of(6, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(22, 0)), result)
    }

    @Test
    fun `window start equal to end means always active`() {
        val reference = instantAt(day, LocalTime.of(3, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 20,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(9, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(3, 20)), result)
    }

    @Test
    fun `interval not evenly dividing window length walks the chain correctly`() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(9, 45)

        val first = NextTriggerCalculator.nextTrigger(
            referenceInstant = instantAt(day, LocalTime.of(9, 0)),
            zone = utc,
            intervalMinutes = 20,
            windowStart = start,
            windowEnd = end,
        )
        assertEquals(instantAt(day, LocalTime.of(9, 20)), first)

        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = first,
            zone = utc,
            intervalMinutes = 20,
            windowStart = start,
            windowEnd = end,
        )
        assertEquals(instantAt(day, LocalTime.of(9, 40)), second)

        val third = NextTriggerCalculator.nextTrigger(
            referenceInstant = second,
            zone = utc,
            intervalMinutes = 20,
            windowStart = start,
            windowEnd = end,
        )
        assertEquals(instantAt(nextDay, LocalTime.of(9, 0)), third)
    }

    @Test
    fun `freshly enabled outside window jumps forward to next window start`() {
        val reference = instantAt(day, LocalTime.of(20, 0)) // "now" at enable time
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 5,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        assertEquals(instantAt(nextDay, LocalTime.of(9, 0)), result)
    }

    // --- Late delivery (doze) catch-up ---

    @Test
    fun `tick delivered late skips the slots that already passed`() {
        val reference = instantAt(day, LocalTime.of(10, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            now = instantAt(day, LocalTime.of(10, 32)), // held by doze for 32 minutes
        )
        // Not 10:15 (already past, would fire instantly) and not 10:47 (restarting from now).
        assertEquals(instantAt(day, LocalTime.of(10, 45)), result)
    }

    @Test
    fun `catch-up keeps the original cadence rather than restarting from now`() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 0)
        val reference = instantAt(day, LocalTime.of(10, 2))

        val first = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = start,
            windowEnd = end,
            now = instantAt(day, LocalTime.of(10, 40)),
        )
        assertEquals(instantAt(day, LocalTime.of(10, 47)), first)

        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = first,
            zone = utc,
            intervalMinutes = 15,
            windowStart = start,
            windowEnd = end,
            now = instantAt(day, LocalTime.of(10, 47)),
        )
        assertEquals(instantAt(day, LocalTime.of(11, 2)), second)
    }

    @Test
    fun `now exactly on a slot boundary advances to the following slot`() {
        val reference = instantAt(day, LocalTime.of(10, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            now = instantAt(day, LocalTime.of(10, 15)),
        )
        assertEquals(instantAt(day, LocalTime.of(10, 30)), result)
    }

    @Test
    fun `catch-up landing past the window end rolls to the next window start`() {
        val reference = instantAt(day, LocalTime.of(16, 30))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            now = instantAt(day, LocalTime.of(19, 5)), // dozed clean past the window
        )
        assertEquals(instantAt(nextDay, LocalTime.of(9, 0)), result)
    }

    @Test
    fun `now before the reference (clock moved back) still advances one interval`() {
        val reference = instantAt(day, LocalTime.of(10, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            now = instantAt(day, LocalTime.of(9, 50)),
        )
        assertEquals(instantAt(day, LocalTime.of(10, 15)), result)
    }

    @Test
    fun `interval below minimum throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            NextTriggerCalculator.nextTrigger(
                referenceInstant = instantAt(day, LocalTime.NOON),
                zone = utc,
                intervalMinutes = 1,
                windowStart = LocalTime.of(9, 0),
                windowEnd = LocalTime.of(17, 0),
            )
        }
    }

    @Test
    fun `interval above maximum throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            NextTriggerCalculator.nextTrigger(
                referenceInstant = instantAt(day, LocalTime.NOON),
                zone = utc,
                intervalMinutes = 121,
                windowStart = LocalTime.of(9, 0),
                windowEnd = LocalTime.of(17, 0),
            )
        }
    }

    @Test
    fun `interval boundary values 2 and 120 are accepted`() {
        val reference = instantAt(day, LocalTime.of(10, 0))
        val twoMin = NextTriggerCalculator.nextTrigger(
            reference, utc, 2, LocalTime.of(9, 0), LocalTime.of(17, 0),
        )
        val twoHours = NextTriggerCalculator.nextTrigger(
            reference, utc, 120, LocalTime.of(9, 0), LocalTime.of(17, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(10, 2)), twoMin)
        assertEquals(instantAt(day, LocalTime.of(12, 0)), twoHours)
    }

    @Test
    fun `interval that is not a multiple of five walks its own cadence`() {
        val start = instantAt(day, LocalTime.of(10, 0))
        val second = NextTriggerCalculator.nextTrigger(
            start, utc, 7, LocalTime.of(9, 0), LocalTime.of(17, 0),
        )
        val third = NextTriggerCalculator.nextTrigger(
            second, utc, 7, LocalTime.of(9, 0), LocalTime.of(17, 0),
        )
        assertEquals(instantAt(day, LocalTime.of(10, 7)), second)
        assertEquals(instantAt(day, LocalTime.of(10, 14)), third)
    }

    // --- DST edge cases (Europe/Amsterdam) ---
    // These assert the calculation is deterministic and doesn't throw when a window boundary
    // falls in a DST gap/overlap; java.time resolves the ambiguity consistently.

    @Test
    fun `DST spring-forward gap at window start resolves deterministically without throwing`() {
        val amsterdam = ZoneId.of("Europe/Amsterdam")
        // 2026-03-29: clocks jump from 02:00 CET to 03:00 CEST; 02:00-03:00 does not exist.
        val gapDay = LocalDate.of(2026, 3, 29)
        val reference = instantAt(gapDay, LocalTime.of(1, 0), amsterdam)

        val first = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = amsterdam,
            intervalMinutes = 5,
            windowStart = LocalTime.of(2, 30),
            windowEnd = LocalTime.of(5, 0),
        )
        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = amsterdam,
            intervalMinutes = 5,
            windowStart = LocalTime.of(2, 30),
            windowEnd = LocalTime.of(5, 0),
        )
        assertEquals(first, second)
    }

    @Test
    fun `DST fall-back overlap at window start resolves deterministically without throwing`() {
        val amsterdam = ZoneId.of("Europe/Amsterdam")
        // 2026-10-25: clocks fall back from 03:00 CEST to 02:00 CET; 02:00-03:00 occurs twice.
        val overlapDay = LocalDate.of(2026, 10, 25)
        val reference = instantAt(overlapDay, LocalTime.of(1, 0), amsterdam)

        val first = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = amsterdam,
            intervalMinutes = 5,
            windowStart = LocalTime.of(2, 30),
            windowEnd = LocalTime.of(5, 0),
        )
        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = amsterdam,
            intervalMinutes = 5,
            windowStart = LocalTime.of(2, 30),
            windowEnd = LocalTime.of(5, 0),
        )
        assertEquals(first, second)
        assertNotEquals(reference, first)
    }
}

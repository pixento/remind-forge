package nl.pixento.remindforge.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random
import nl.pixento.remindforge.domain.model.DailyWindow
import nl.pixento.remindforge.domain.model.IntervalRandomness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
        )
        assertEquals(instantAt(day, LocalTime.of(9, 0)), result)
    }

    @Test
    fun `non-wrap window, naive after end jumps to tomorrow's start`() {
        // Also the freshly-enabled-outside-the-window case: on enable the reference *is* "now".
        val reference = instantAt(day, LocalTime.of(20, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 10,
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
            window = DailyWindow(LocalTime.of(22, 0), LocalTime.of(6, 0)),
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
            window = DailyWindow(LocalTime.of(22, 0), LocalTime.of(6, 0)),
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
            window = DailyWindow(LocalTime.of(22, 0), LocalTime.of(6, 0)),
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
            window = DailyWindow(LocalTime.of(22, 0), LocalTime.of(6, 0)),
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(9, 0)),
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
            window = DailyWindow(start, end),
        )
        assertEquals(instantAt(day, LocalTime.of(9, 20)), first)

        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = first,
            zone = utc,
            intervalMinutes = 20,
            window = DailyWindow(start, end),
        )
        assertEquals(instantAt(day, LocalTime.of(9, 40)), second)

        val third = NextTriggerCalculator.nextTrigger(
            referenceInstant = second,
            zone = utc,
            intervalMinutes = 20,
            window = DailyWindow(start, end),
        )
        assertEquals(instantAt(nextDay, LocalTime.of(9, 0)), third)
    }

    // --- No window at all (the Do-Not-Disturb-following mode) ---

    @Test
    fun `null window never clamps, whatever the time of day`() {
        // 03:00 is outside the default 09:00-17:00 window, so a window would have pushed this to
        // 09:00; with none, the slot stands.
        val reference = instantAt(day, LocalTime.of(3, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            window = null,
        )
        assertEquals(instantAt(day, LocalTime.of(3, 15)), result)
    }

    @Test
    fun `null window still skips slots missed during a late delivery`() {
        val reference = instantAt(day, LocalTime.of(3, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            window = null,
            now = instantAt(day, LocalTime.of(3, 32)),
        )
        assertEquals(instantAt(day, LocalTime.of(3, 45)), result)
    }

    @Test
    fun `null window makes every instant within window`() {
        assertTrue(
            NextTriggerCalculator.isWithinWindow(
                instantAt(day, LocalTime.of(3, 0)),
                utc,
                window = null,
            ),
        )
    }

    @Test
    fun `isWithinWindow applies the window when there is one`() {
        val window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertTrue(
            NextTriggerCalculator.isWithinWindow(instantAt(day, LocalTime.of(10, 0)), utc, window),
        )
        assertFalse(
            NextTriggerCalculator.isWithinWindow(instantAt(day, LocalTime.of(3, 0)), utc, window),
        )
    }

    // --- Late delivery (doze) catch-up ---

    @Test
    fun `tick delivered late skips the slots that already passed`() {
        val reference = instantAt(day, LocalTime.of(10, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 15,
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
            window = DailyWindow(start, end),
            now = instantAt(day, LocalTime.of(10, 40)),
        )
        assertEquals(instantAt(day, LocalTime.of(10, 47)), first)

        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = first,
            zone = utc,
            intervalMinutes = 15,
            window = DailyWindow(start, end),
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
                window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
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
                window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
            )
        }
    }

    @Test
    fun `any accepted interval walks its own cadence, boundaries included`() {
        // The interval is free numeric entry, not a preset: nothing snaps it to a five-minute grid,
        // and both ends of the accepted 2..120 range work.
        val reference = instantAt(day, LocalTime.of(10, 0))
        val expected = mapOf(
            2 to LocalTime.of(10, 2),
            7 to LocalTime.of(10, 7),
            120 to LocalTime.of(12, 0),
        )

        expected.forEach { (intervalMinutes, expectedTime) ->
            val result = NextTriggerCalculator.nextTrigger(
                reference,
                utc,
                intervalMinutes,
                DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
            )
            assertEquals("interval $intervalMinutes", instantAt(day, expectedTime), result)
        }
    }

    // --- Interval randomness ---
    // The draw is pinned to one end of its range rather than seeded, so each assertion names the
    // instant it expects instead of whatever a particular PRNG implementation happens to produce.

    @Test
    fun `no randomness leaves the interval exact and never draws`() {
        val reference = instantAt(day, LocalTime.of(9, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 5,
            window = null,
            randomness = IntervalRandomness.NONE,
            random = PinnedRandom { _, _ ->
                throw AssertionError("NONE must not consult the random source")
            },
        )
        assertEquals(instantAt(day, LocalTime.of(9, 5)), result)
    }

    @Test
    fun `the shortest draw is the interval minus the full deviation`() {
        val reference = instantAt(day, LocalTime.of(9, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 5,
            window = null,
            randomness = IntervalRandomness.TEN_PERCENT,
            random = PinnedRandom.Shortest,
        )
        assertEquals(instantAt(day, LocalTime.of(9, 4, 30)), result)
    }

    @Test
    fun `the longest draw is the interval plus the full deviation`() {
        // Also pins the exclusive-upper-bound detail: +deviation itself has to be reachable.
        val reference = instantAt(day, LocalTime.of(9, 0))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 5,
            window = null,
            randomness = IntervalRandomness.FIFTY_PERCENT,
            random = PinnedRandom.Longest,
        )
        assertEquals(instantAt(day, LocalTime.of(9, 7, 30)), result)
    }

    @Test
    fun `every draw stays inside the advertised range`() {
        val reference = instantAt(day, LocalTime.of(9, 0))
        val random = Random(20260814)

        repeat(500) {
            val result = NextTriggerCalculator.nextTrigger(
                referenceInstant = reference,
                zone = utc,
                intervalMinutes = 20,
                window = null,
                randomness = IntervalRandomness.TWENTY_PERCENT,
                random = random,
            )
            assertTrue(
                "$result outside 09:16-09:24",
                !result.isBefore(instantAt(day, LocalTime.of(9, 16))) &&
                        !result.isAfter(instantAt(day, LocalTime.of(9, 24))),
            )
        }
    }

    @Test
    fun `a jittered slot past the window end still clamps to the next window start`() {
        // The deviation makes the gap unpredictable, not the window: the first reminder of the day
        // stays exactly on the window start, which is what re-anchors the drifting chain daily.
        val reference = instantAt(day, LocalTime.of(16, 58))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 5,
            window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)),
            randomness = IntervalRandomness.TEN_PERCENT,
            random = PinnedRandom.Longest,
        )
        assertEquals(instantAt(nextDay, LocalTime.of(9, 0)), result)
    }

    @Test
    fun `a late tick catches up on the jittered cadence and never lands in the past`() {
        val reference = instantAt(day, LocalTime.of(10, 0))
        val now = instantAt(day, LocalTime.of(10, 20))
        val result = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = utc,
            intervalMinutes = 5,
            window = null,
            now = now,
            randomness = IntervalRandomness.TEN_PERCENT,
            random = PinnedRandom.Shortest,
        )
        // Five 4:30 gaps from 10:00 - the first slot on that cadence still ahead of 10:20.
        assertEquals(instantAt(day, LocalTime.of(10, 22, 30)), result)
        assertTrue(result.isAfter(now))
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
            window = DailyWindow(LocalTime.of(2, 30), LocalTime.of(5, 0)),
        )
        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = amsterdam,
            intervalMinutes = 5,
            window = DailyWindow(LocalTime.of(2, 30), LocalTime.of(5, 0)),
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
            window = DailyWindow(LocalTime.of(2, 30), LocalTime.of(5, 0)),
        )
        val second = NextTriggerCalculator.nextTrigger(
            referenceInstant = reference,
            zone = amsterdam,
            intervalMinutes = 5,
            window = DailyWindow(LocalTime.of(2, 30), LocalTime.of(5, 0)),
        )
        assertEquals(first, second)
        assertNotEquals(reference, first)
    }
}

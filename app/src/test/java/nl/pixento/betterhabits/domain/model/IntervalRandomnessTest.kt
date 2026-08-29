package nl.pixento.betterhabits.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The deviation is what both the alarm chain and the settings row are built on, so its rounding is
 * the difference between a row reading "Every 5:25 - 6:35 minutes" and one reading "5:24 - 6:36".
 */
class IntervalRandomnessTest {

    @Test
    fun `no randomness has no deviation at any interval`() {
        listOf(2, 5, 15, 120).forEach { minutes ->
            assertEquals("interval $minutes", 0L, IntervalRandomness.NONE.deviationSeconds(minutes))
        }
    }

    @Test
    fun `an exact percentage is left alone when it already lands on a five-second step`() {
        // 10% of 5 minutes is 30s, 20% of 15 is 180s - nothing to round.
        assertEquals(30L, IntervalRandomness.TEN_PERCENT.deviationSeconds(5))
        assertEquals(180L, IntervalRandomness.TWENTY_PERCENT.deviationSeconds(15))
    }

    @Test
    fun `an awkward percentage rounds to the nearest five-second step`() {
        // 10% of 6 minutes is 36s -> 35s, so the row reads 5:25 - 6:35 rather than 5:24 - 6:36.
        assertEquals(35L, IntervalRandomness.TEN_PERCENT.deviationSeconds(6))
        // 42s rounds down to 40, 48s up to 50: the step is nearest, not floor.
        assertEquals(40L, IntervalRandomness.TEN_PERCENT.deviationSeconds(7))
        assertEquals(50L, IntervalRandomness.TWENTY_PERCENT.deviationSeconds(4))
    }

    @Test
    fun `fifty percent of a whole minute always lands on a step already`() {
        (ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES)
            .forEach { minutes ->
                val deviation = IntervalRandomness.FIFTY_PERCENT.deviationSeconds(minutes)
                assertEquals("interval $minutes", minutes * 30L, deviation)
            }
    }

    @Test
    fun `every deviation is a positive multiple of the step across the whole interval range`() {
        // Rounding must never swallow a randomness the user explicitly picked, and the smallest
        // possible deviation - 10% of the two-minute minimum, 12s - is the case that could.
        (ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES)
            .forEach { minutes ->
                IntervalRandomness.entries
                    .filter { it != IntervalRandomness.NONE }
                    .forEach { randomness ->
                        val deviation = randomness.deviationSeconds(minutes)
                        val label = "$randomness at $minutes minutes"
                        assertTrue("$label was $deviation", deviation > 0)
                        assertEquals(
                            label,
                            0L,
                            deviation % IntervalRandomness.DEVIATION_STEP_SECONDS,
                        )
                    }
            }
    }

    @Test
    fun `the deviation never reaches the interval itself, so a gap is always positive`() {
        (ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES)
            .forEach { minutes ->
                IntervalRandomness.entries.forEach { randomness ->
                    assertTrue(
                        "$randomness at $minutes minutes",
                        randomness.deviationSeconds(minutes) < minutes * 60L,
                    )
                }
            }
    }
}

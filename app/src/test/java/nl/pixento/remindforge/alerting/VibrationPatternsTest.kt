package nl.pixento.remindforge.alerting

import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationPatternsTest {

    @Test
    fun `short pulse waveform`() {
        assertArrayEquals(
            longArrayOf(0, 150),
            VibrationPatterns.waveformFor(VibrationPatternType.SHORT_PULSE)
        )
    }

    @Test
    fun `long pulse waveform`() {
        assertArrayEquals(
            longArrayOf(0, 700),
            VibrationPatterns.waveformFor(VibrationPatternType.LONG_PULSE)
        )
    }

    @Test
    fun `double pulse waveform`() {
        assertArrayEquals(
            longArrayOf(0, 150, 250, 150),
            VibrationPatterns.waveformFor(VibrationPatternType.DOUBLE_PULSE),
        )
    }

    @Test
    fun `triple pulse waveform`() {
        assertArrayEquals(
            longArrayOf(0, 150, 250, 150, 250, 150),
            VibrationPatterns.waveformFor(VibrationPatternType.TRIPLE_PULSE),
        )
    }

    @Test
    fun `counted pulses are separated by a wider gap than their own buzz`() {
        // What makes two taps countable rather than one smeared buzz: the silence between beats
        // has to be at least as long as the beats themselves.
        listOf(VibrationPatternType.DOUBLE_PULSE, VibrationPatternType.TRIPLE_PULSE)
            .forEach { pattern ->
                val waveform = VibrationPatterns.waveformFor(pattern)!!
                val buzzes = waveform.filterIndexed { i, _ -> i % 2 == 1 }
                val gaps = waveform.filterIndexed { i, _ -> i > 0 && i % 2 == 0 }
                assertTrue(
                    "$pattern: gaps $gaps vs buzzes $buzzes",
                    gaps.min() >= buzzes.max(),
                )
            }
    }

    @Test
    fun `long-short-short waveform`() {
        assertArrayEquals(
            longArrayOf(0, 500, 250, 150, 250, 150),
            VibrationPatterns.waveformFor(VibrationPatternType.LONG_SHORT_SHORT),
        )
    }

    @Test
    fun `short-short-long waveform`() {
        assertArrayEquals(
            longArrayOf(0, 150, 250, 150, 250, 500),
            VibrationPatterns.waveformFor(VibrationPatternType.SHORT_SHORT_LONG),
        )
    }

    @Test
    fun `short-long-short waveform`() {
        assertArrayEquals(
            longArrayOf(0, 150, 250, 500, 250, 150),
            VibrationPatterns.waveformFor(VibrationPatternType.SHORT_LONG_SHORT),
        )
    }

    @Test
    fun `long-short-long waveform`() {
        assertArrayEquals(
            longArrayOf(0, 500, 250, 150, 250, 500),
            VibrationPatterns.waveformFor(VibrationPatternType.LONG_SHORT_LONG),
        )
    }

    @Test
    fun `audible patterns are all distinguishable from each other`() {
        val waveforms = VibrationPatternType.entries
            .filter { it != VibrationPatternType.SILENT }
            .map { VibrationPatterns.waveformFor(it)!!.toList() }

        assertEquals(waveforms.size, waveforms.distinct().size)
    }

    @Test
    fun `silent has no waveform`() {
        assertNull(VibrationPatterns.waveformFor(VibrationPatternType.SILENT))
    }
}

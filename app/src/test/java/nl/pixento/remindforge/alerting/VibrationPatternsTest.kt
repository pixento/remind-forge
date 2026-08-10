package nl.pixento.remindforge.alerting

import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertArrayEquals
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
            longArrayOf(0, 150, 150, 150),
            VibrationPatterns.waveformFor(VibrationPatternType.DOUBLE_PULSE),
        )
    }

    @Test
    fun `triple pulse waveform`() {
        assertArrayEquals(
            longArrayOf(0, 120, 120, 120, 120, 120),
            VibrationPatterns.waveformFor(VibrationPatternType.TRIPLE_PULSE),
        )
    }

    @Test
    fun `every pattern type has a waveform mapped`() {
        VibrationPatternType.entries.forEach { pattern ->
            assert(VibrationPatterns.waveformFor(pattern).isNotEmpty())
        }
    }
}

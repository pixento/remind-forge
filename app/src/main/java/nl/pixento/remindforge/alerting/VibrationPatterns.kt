package nl.pixento.remindforge.alerting

import nl.pixento.remindforge.domain.model.VibrationPatternType

object VibrationPatterns {

    /** [delay, on, off, on, ...] waveform arrays consumed by VibrationEffect/Vibrator. */
    fun waveformFor(pattern: VibrationPatternType): LongArray = when (pattern) {
        VibrationPatternType.SHORT_PULSE -> longArrayOf(0, 150)
        VibrationPatternType.LONG_PULSE -> longArrayOf(0, 700)
        VibrationPatternType.DOUBLE_PULSE -> longArrayOf(0, 150, 150, 150)
        VibrationPatternType.TRIPLE_PULSE -> longArrayOf(0, 120, 120, 120, 120, 120)
    }
}

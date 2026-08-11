package nl.pixento.remindforge.alerting

import nl.pixento.remindforge.domain.model.VibrationPatternType

object VibrationPatterns {

    /**
     * [delay, on, off, on, ...] waveform arrays consumed by VibrationEffect/Vibrator, or null for
     * [VibrationPatternType.SILENT] - "don't vibrate" as one explicit concept rather than a
     * zero-length waveform handed to the vibrator.
     */
    fun waveformFor(pattern: VibrationPatternType): LongArray? = when (pattern) {
        VibrationPatternType.SHORT_PULSE -> longArrayOf(0, 150)
        VibrationPatternType.LONG_PULSE -> longArrayOf(0, 700)
        VibrationPatternType.DOUBLE_PULSE -> longArrayOf(0, 150, 150, 150)
        VibrationPatternType.TRIPLE_PULSE -> longArrayOf(0, 120, 120, 120, 120, 120)
        VibrationPatternType.SILENT -> null
    }
}

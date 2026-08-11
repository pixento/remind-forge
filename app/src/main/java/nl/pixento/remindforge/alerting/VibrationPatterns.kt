package nl.pixento.remindforge.alerting

import nl.pixento.remindforge.domain.model.VibrationPatternType

object VibrationPatterns {

    /**
     * [delay, on, off, on, ...] waveform arrays consumed by VibrationEffect/Vibrator, or null for
     * [VibrationPatternType.SILENT] - "don't vibrate" as one explicit concept rather than a
     * zero-length waveform handed to the vibrator.
     */
    fun waveformFor(pattern: VibrationPatternType): LongArray? = when (pattern) {
        VibrationPatternType.SHORT_PULSE -> longArrayOf(0, SHORT)
        VibrationPatternType.LONG_PULSE -> longArrayOf(0, 700)
        // Counted pulses: identical SHORT beats, so the only thing to perceive is how many there
        // are - that needs a GAP wide enough to stop two taps blurring into one long buzz, which
        // is what made the old 120-150ms spacing hard to tell apart.
        VibrationPatternType.DOUBLE_PULSE -> longArrayOf(0, SHORT, GAP, SHORT)
        VibrationPatternType.TRIPLE_PULSE -> longArrayOf(0, SHORT, GAP, SHORT, GAP, SHORT)
        // Mixed rhythms: SHORT and LONG buzzes separated by GAP, so the long beat is ~3x the
        // short one and the rhythm stays readable through a pocket.
        VibrationPatternType.LONG_SHORT_SHORT -> longArrayOf(0, LONG, GAP, SHORT, GAP, SHORT)
        VibrationPatternType.SHORT_SHORT_LONG -> longArrayOf(0, SHORT, GAP, SHORT, GAP, LONG)
        VibrationPatternType.SHORT_LONG_SHORT -> longArrayOf(0, SHORT, GAP, LONG, GAP, SHORT)
        VibrationPatternType.LONG_SHORT_LONG -> longArrayOf(0, LONG, GAP, SHORT, GAP, LONG)
        VibrationPatternType.SILENT -> null
    }

    private const val SHORT = 150L
    private const val LONG = 500L
    private const val GAP = 250L
}

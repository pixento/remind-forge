package nl.pixento.remindforge.domain.model

enum class VibrationPatternType(val label: String) {
    SHORT_PULSE("Short pulse"),
    LONG_PULSE("Long pulse"),
    DOUBLE_PULSE("Double pulse"),
    TRIPLE_PULSE("Triple pulse"),

    // Mixed-length rhythms - easier to tell apart at a glance than the evenly-spaced pulses above,
    // so a buzz in your pocket is recognisable without looking at the phone.
    LONG_SHORT_SHORT("Long-short-short"),
    SHORT_SHORT_LONG("Short-short-long"),
    SHORT_LONG_SHORT("Short-long-short"),
    LONG_SHORT_LONG("Long-short-long"),

    /** No buzz at all - the way to silence the vibration channel while keeping a ringtone. */
    SILENT("Silent"),
}

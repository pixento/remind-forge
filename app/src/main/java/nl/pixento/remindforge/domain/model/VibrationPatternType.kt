package nl.pixento.remindforge.domain.model

enum class VibrationPatternType(val label: String) {
    SHORT_PULSE("Short pulse"),
    LONG_PULSE("Long pulse"),
    DOUBLE_PULSE("Double pulse"),
    TRIPLE_PULSE("Triple pulse"),

    /** No buzz at all - the way to silence the vibration channel while keeping a ringtone. */
    SILENT("Silent"),
}

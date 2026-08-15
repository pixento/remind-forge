package nl.pixento.remindforge.domain.model

// Display labels live in the UI layer (ui/settings/vibration/VibrationPatternLabels.kt) as string
// resources, keeping this enum framework-free and translatable.
enum class VibrationPatternType {
    SHORT_PULSE,
    LONG_PULSE,
    DOUBLE_PULSE,
    TRIPLE_PULSE,

    // Mixed-length rhythms - easier to tell apart at a glance than the evenly-spaced pulses above,
    // so a buzz in your pocket is recognisable without looking at the phone.
    LONG_SHORT_SHORT,
    SHORT_SHORT_LONG,
    SHORT_LONG_SHORT,
    LONG_SHORT_LONG,
    LONG_SHORT_SHORT_SHORT,
    SHORT_SHORT_SHORT_LONG,

    /** No buzz at all - the way to silence the vibration channel while keeping a ringtone. */
    SILENT,
}

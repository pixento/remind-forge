package nl.pixento.remindforge.domain.model

// Display labels live in the UI layer (ui/settings/components/IntervalPicker.kt) as a string
// resource, keeping this enum framework-free and translatable.
/**
 * How much each gap between reminders may vary from the configured interval, as a percentage of it.
 * A predictable metronome is easy to tune out, so varying the gap makes a reminder harder to
 * anticipate - which is the point for the habit-forming use this app is for.
 */
enum class IntervalRandomness(val percent: Int) {
    NONE(0),
    TEN_PERCENT(10),
    TWENTY_PERCENT(20),
    FIFTY_PERCENT(50);

    /**
     * The largest deviation either side of the interval, in whole seconds. Exact for every
     * percentage here: 10/20/50% of a whole number of minutes always lands on whole seconds.
     */
    fun deviationSeconds(intervalMinutes: Int): Long =
        intervalMinutes * 60L * percent / 100
}

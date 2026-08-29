package nl.pixento.betterhabits.domain.model

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
     * The largest deviation either side of the interval, in whole seconds, rounded to the nearest
     * multiple of [DEVIATION_STEP_SECONDS].
     *
     * The exact percentage of an arbitrary interval lands on awkward numbers - 10% of 6 minutes is
     * 36 seconds, which the settings row would advertise as "Every 5:24 - 6:36 minutes". Snapping
     * the bound to a five-second step costs nothing (the deviation is a rough dial, not a promise
     * to the second) and buys a range that reads like a time rather than a calculation.
     */
    fun deviationSeconds(intervalMinutes: Int): Long {
        val exact = intervalMinutes * 60L * percent / 100
        return (exact + DEVIATION_STEP_SECONDS / 2) / DEVIATION_STEP_SECONDS * DEVIATION_STEP_SECONDS
    }

    companion object {
        /**
         * Never rounds a non-zero deviation away to nothing: the smallest one any option can
         * produce is 10% of the two-minute minimum, i.e. 12 seconds.
         */
        const val DEVIATION_STEP_SECONDS = 5L
    }
}

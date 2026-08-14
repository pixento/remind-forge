package nl.pixento.remindforge.ui.settings.components

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.IntervalRandomness
import nl.pixento.remindforge.domain.model.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class IntervalPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private var chosen: Pair<Int, IntervalRandomness>? = null

    private fun intervalValue(minutes: Int) = context.getString(R.string.interval_value, minutes)

    private fun rangeValue(low: String, high: String) =
        context.getString(R.string.interval_value_range, low, high)

    private fun randomnessLabel(randomness: IntervalRandomness) =
        context.getString(R.string.interval_randomness_value, randomness.percent)

    private fun setPicker(
        current: Int = 15,
        randomness: IntervalRandomness = IntervalRandomness.NONE,
    ) {
        composeRule.setContent {
            IntervalPicker(
                intervalMinutes = current,
                randomness = randomness,
                onIntervalChange = { minutes, picked -> chosen = minutes to picked },
            )
        }
    }

    /** Taps the collapsed row (which reads [current]) and returns the dialog's number field. */
    private fun tapRow(current: Int = 15) = run {
        composeRule.onNodeWithText(intervalValue(current)).performClick()
        composeRule.onNode(hasSetTextAction())
    }

    /** Mounts the picker and opens its dialog on [current], returning the number field. */
    private fun openDialog(current: Int = 15) = run {
        setPicker(current)
        tapRow(current)
    }

    private fun okButton() = composeRule.onNodeWithText(context.getString(R.string.dialog_ok))

    private fun hintText() = context.getString(
        R.string.interval_input_hint,
        ReminderSettings.MIN_INTERVAL_MINUTES,
        ReminderSettings.MAX_INTERVAL_MINUTES,
    )

    private fun errorText() = context.getString(
        R.string.interval_input_error,
        ReminderSettings.MIN_INTERVAL_MINUTES,
        ReminderSettings.MAX_INTERVAL_MINUTES,
    )

    @Test
    fun dialogOpensOnTheCurrentValue() {
        val field = openDialog(current = 15)

        // The field node merges its supporting text, so assert on the editable value only.
        field.assertTextContains("15")
        composeRule.onNodeWithText(hintText()).assertExists()
        okButton().assertIsEnabled()
    }

    @Test
    fun okCommitsAnyValueInTheAcceptedRange() {
        // Entry is a free number rather than a preset, so what matters is the whole range: an
        // everyday value, one off the old five-minute grid, and both ends of 2..120.
        setPicker(current = 15)

        listOf(45, 7, ReminderSettings.MIN_INTERVAL_MINUTES, ReminderSettings.MAX_INTERVAL_MINUTES)
            .forEach { minutes ->
                chosen = null

                // The row is stateless here, so it still reads 15 after each commit.
                tapRow().performTextReplacement(minutes.toString())
                okButton().performClick()

                assertEquals(minutes to IntervalRandomness.NONE, chosen)
            }
    }

    @Test
    fun cancelDiscardsTheTypedInterval() {
        val field = openDialog()

        field.performTextReplacement("45")
        composeRule.onNodeWithText(context.getString(R.string.dialog_cancel)).performClick()

        assertNull(chosen)
    }

    @Test
    fun okIsBlockedBelowTheMinimum() {
        val field = openDialog()

        field.performTextReplacement((ReminderSettings.MIN_INTERVAL_MINUTES - 1).toString())

        okButton().assertIsNotEnabled()
        composeRule.onNodeWithText(errorText()).assertExists()
        assertNull(chosen)
    }

    @Test
    fun okIsBlockedWhileTheFieldIsEmpty() {
        val field = openDialog()

        field.performTextClearance()

        okButton().assertIsNotEnabled()
        // An empty field is incomplete, not wrong, so it keeps the hint instead of erroring.
        composeRule.onNodeWithText(hintText()).assertExists()
        assertNull(chosen)
    }

    @Test
    fun nonDigitsAreRejected() {
        val field = openDialog()

        field.performTextClearance()
        field.performTextInput("4a5")

        field.assertTextContains("45")
    }

    // --- Randomness ---

    @Test
    fun everyRandomnessOptionIsOfferedWithTheCurrentOneSelected() {
        setPicker(current = 15, randomness = IntervalRandomness.TWENTY_PERCENT)
        composeRule.onNodeWithText(rangeValue("12", "18")).performClick()

        IntervalRandomness.entries.forEach { option ->
            composeRule.onNodeWithText(randomnessLabel(option)).assertExists()
        }
        composeRule.onNodeWithText(randomnessLabel(IntervalRandomness.TWENTY_PERCENT))
            .assertIsSelected()
    }

    @Test
    fun okCommitsTheIntervalAndTheRandomnessTogether() {
        // One callback for both, so the caller writes them in a single persist and the alarm chain
        // restarts once rather than twice.
        val field = openDialog(current = 15)

        field.performTextReplacement("5")
        composeRule.onNodeWithText(randomnessLabel(IntervalRandomness.TEN_PERCENT)).performClick()
        okButton().performClick()

        assertEquals(5 to IntervalRandomness.TEN_PERCENT, chosen)
    }

    @Test
    fun cancelDiscardsThePickedRandomness() {
        openDialog(current = 15)

        composeRule.onNodeWithText(randomnessLabel(IntervalRandomness.FIFTY_PERCENT)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.dialog_cancel)).performClick()

        assertNull(chosen)
        // The row still reads the unchanged exact interval rather than a range.
        composeRule.onNodeWithText(intervalValue(15)).assertExists()
    }

    @Test
    fun theRowShowsAnExactIntervalWithoutRandomness() {
        setPicker(current = 15, randomness = IntervalRandomness.NONE)
        composeRule.onNodeWithText(intervalValue(15)).assertExists()
    }

    @Test
    fun theRowShowsARangeOnceRandomnessIsOn() {
        // Seconds are only spelled out when the bound has any, so 15 +/- 20% stays whole minutes
        // while 5 +/- 10% needs the half minute.
        setPicker(current = 15, randomness = IntervalRandomness.TWENTY_PERCENT)
        composeRule.onNodeWithText(rangeValue("12", "18")).assertExists()
    }

    @Test
    fun aRangeBoundWithSecondsSpellsThemOut() {
        setPicker(current = 5, randomness = IntervalRandomness.TEN_PERCENT)
        composeRule.onNodeWithText(rangeValue("4:30", "5:30")).assertExists()
    }

    @Test
    fun anAwkwardPercentageStillReadsAsWholeFiveSecondSteps() {
        // 10% of 6 minutes is 36s exactly, which would read as 5:24 - 6:36.
        setPicker(current = 6, randomness = IntervalRandomness.TEN_PERCENT)
        composeRule.onNodeWithText(rangeValue("5:25", "6:35")).assertExists()
    }
}

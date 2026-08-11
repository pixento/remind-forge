package nl.pixento.remindforge.ui.settings.components

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import nl.pixento.remindforge.domain.model.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class IntervalPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private var chosen: Int? = null

    private fun intervalValue(minutes: Int) = context.getString(R.string.interval_value, minutes)

    private fun setPicker(current: Int = 15) {
        composeRule.setContent {
            IntervalPicker(intervalMinutes = current, onIntervalChange = { chosen = it })
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
    fun rowShowsTheCurrentInterval() {
        setPicker(current = 15)
        composeRule.onNodeWithText(intervalValue(15)).assertExists()
    }

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

                assertEquals(minutes, chosen)
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
    fun okIsBlockedAboveTheMaximum() {
        val field = openDialog()

        field.performTextReplacement("999")

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
}

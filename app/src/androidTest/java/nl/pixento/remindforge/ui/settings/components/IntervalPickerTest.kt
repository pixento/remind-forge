package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import nl.pixento.remindforge.domain.model.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class IntervalPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var chosen: Int? = null

    private fun setPicker(current: Int = 15) {
        composeRule.setContent {
            IntervalPicker(intervalMinutes = current, onIntervalChange = { chosen = it })
        }
    }

    @Test
    fun rowShowsTheCurrentInterval() {
        setPicker(current = 15)
        composeRule.onNodeWithText("Every 15 minutes").assertExists()
    }

    @Test
    fun okCommitsTheTappedInterval() {
        setPicker()
        composeRule.onNodeWithText("Every 15 minutes").performClick()

        composeRule.onNodeWithText("Every 20 minutes").performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(20, chosen)
    }

    @Test
    fun cancelDiscardsTheTappedInterval() {
        setPicker()
        composeRule.onNodeWithText("Every 15 minutes").performClick()

        composeRule.onNodeWithText("Every 20 minutes").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertNull(chosen)
    }

    @Test
    fun offeredIntervalsStayInsideTheSupportedRange() {
        setPicker()
        composeRule.onNodeWithText("Every 15 minutes").performClick()

        composeRule
            .onNodeWithText("Every ${ReminderSettings.MIN_INTERVAL_MINUTES} minutes")
            .assertExists()
        composeRule
            .onNodeWithText("Every ${ReminderSettings.MAX_INTERVAL_MINUTES} minutes")
            .assertExists()
        composeRule
            .onNodeWithText("Every ${ReminderSettings.MAX_INTERVAL_MINUTES + 5} minutes")
            .assertDoesNotExist()
    }
}

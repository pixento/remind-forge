package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IntervalStepperTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clampsAtMinimum() {
        var value = 5
        composeRule.setContent {
            IntervalStepper(intervalMinutes = value, onIntervalChange = { value = it })
        }
        composeRule.onNodeWithText("-").performClick()
        assertEquals(5, value)
    }

    @Test
    fun clampsAtMaximum() {
        var value = 30
        composeRule.setContent {
            IntervalStepper(intervalMinutes = value, onIntervalChange = { value = it })
        }
        composeRule.onNodeWithText("+").performClick()
        assertEquals(30, value)
    }

    @Test
    fun incrementsWithinRange() {
        var value = 15
        composeRule.setContent {
            IntervalStepper(intervalMinutes = value, onIntervalChange = { value = it })
        }
        composeRule.onNodeWithText("+").performClick()
        assertEquals(20, value)
    }

    @Test
    fun decrementsWithinRange() {
        var value = 15
        composeRule.setContent {
            IntervalStepper(intervalMinutes = value, onIntervalChange = { value = it })
        }
        composeRule.onNodeWithText("-").performClick()
        assertEquals(10, value)
    }
}

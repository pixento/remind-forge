package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class TimeWindowPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overnightWrapLabelShownWhenStartAfterEnd() {
        composeRule.setContent {
            TimeWindowPicker(
                windowStart = LocalTime.of(22, 0),
                windowEnd = LocalTime.of(6, 0),
                onWindowStartChange = {},
                onWindowEndChange = {},
            )
        }
        composeRule.onNodeWithText("Overnight window (wraps past midnight)").assertExists()
    }

    @Test
    fun overnightWrapLabelHiddenForSameDayWindow() {
        composeRule.setContent {
            TimeWindowPicker(
                windowStart = LocalTime.of(9, 0),
                windowEnd = LocalTime.of(17, 0),
                onWindowStartChange = {},
                onWindowEndChange = {},
            )
        }
        composeRule.onNodeWithText("Overnight window (wraps past midnight)").assertDoesNotExist()
    }

    @Test
    fun timeButtonsShowFormattedValues() {
        composeRule.setContent {
            TimeWindowPicker(
                windowStart = LocalTime.of(8, 5),
                windowEnd = LocalTime.of(20, 30),
                onWindowStartChange = {},
                onWindowEndChange = {},
            )
        }
        composeRule.onNodeWithText("Start: 08:05").assertExists()
        composeRule.onNodeWithText("End: 20:30").assertExists()
    }
}

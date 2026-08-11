package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class TimeWindowPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setPicker(windowStart: LocalTime, windowEnd: LocalTime) {
        composeRule.setContent {
            TimeWindowPicker(
                windowStart = windowStart,
                windowEnd = windowEnd,
                onWindowStartChange = {},
                onWindowEndChange = {},
            )
        }
    }

    @Test
    fun rowsShowFormattedTimes() {
        setPicker(windowStart = LocalTime.of(8, 5), windowEnd = LocalTime.of(20, 30))

        composeRule.onNodeWithText("Start time").assertExists()
        composeRule.onNodeWithText("08:05").assertExists()
        composeRule.onNodeWithText("End time").assertExists()
        composeRule.onNodeWithText("20:30").assertExists()
    }

    @Test
    fun endTimeIsMarkedNextDayWhenTheWindowWrapsPastMidnight() {
        setPicker(windowStart = LocalTime.of(22, 0), windowEnd = LocalTime.of(6, 0))

        composeRule.onNodeWithText("06:00 (next day)").assertExists()
    }

    @Test
    fun endTimeIsNotMarkedNextDayForASameDayWindow() {
        setPicker(windowStart = LocalTime.of(9, 0), windowEnd = LocalTime.of(17, 0))

        composeRule.onNodeWithText("17:00").assertExists()
        composeRule.onNodeWithText("17:00 (next day)").assertDoesNotExist()
    }
}

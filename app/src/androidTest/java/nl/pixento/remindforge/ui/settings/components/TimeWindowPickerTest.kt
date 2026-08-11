package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import java.time.LocalTime
import nl.pixento.remindforge.R
import org.junit.Rule
import org.junit.Test

class TimeWindowPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

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

        composeRule.onNodeWithText(context.getString(R.string.time_window_start)).assertExists()
        composeRule.onNodeWithText("08:05").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.time_window_end)).assertExists()
        composeRule.onNodeWithText("20:30").assertExists()
    }

    @Test
    fun endTimeIsMarkedNextDayWhenTheWindowWrapsPastMidnight() {
        setPicker(windowStart = LocalTime.of(22, 0), windowEnd = LocalTime.of(6, 0))

        val suffix = context.getString(R.string.time_window_next_day_suffix)
        composeRule.onNodeWithText("06:00$suffix").assertExists()
    }

    @Test
    fun endTimeIsNotMarkedNextDayForASameDayWindow() {
        setPicker(windowStart = LocalTime.of(9, 0), windowEnd = LocalTime.of(17, 0))

        val suffix = context.getString(R.string.time_window_next_day_suffix)
        composeRule.onNodeWithText("17:00").assertExists()
        composeRule.onNodeWithText("17:00$suffix").assertDoesNotExist()
    }
}

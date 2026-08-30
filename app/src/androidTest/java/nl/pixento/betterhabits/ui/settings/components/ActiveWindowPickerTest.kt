package nl.pixento.betterhabits.ui.settings.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import java.time.LocalTime
import nl.pixento.betterhabits.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActiveWindowPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun string(resId: Int) = context.getString(resId)

    private fun setPicker(
        limitToActiveHours: Boolean,
        onLimitToActiveHoursChange: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            // The picker emits a run of sibling rows, so it needs the same Column its SettingsGroup
            // gives it on the real screen. Stack them instead and every row overlaps, leaving the
            // last one to swallow clicks meant for the others.
            SettingsGroup {
                ActiveWindowPicker(
                    limitToActiveHours = limitToActiveHours,
                    windowStart = LocalTime.of(9, 0),
                    windowEnd = LocalTime.of(17, 0),
                    onLimitToActiveHoursChange = onLimitToActiveHoursChange,
                    onWindowStartChange = {},
                    onWindowEndChange = {},
                )
            }
        }
    }

    @Test
    fun setHoursLeavesTheTimeRowsLive() {
        setPicker(limitToActiveHours = true)

        composeRule.onNodeWithText(string(R.string.limit_to_active_hours)).assertIsOn()
        composeRule.onNodeWithText(string(R.string.time_window_start)).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.time_window_end)).assertIsEnabled()
    }

    @Test
    fun droppingTheHoursKeepsTheTimeRowsVisibleButDisabled() {
        // The times still show their stored values - re-ticking restores them - but they no longer
        // decide anything, so they must not read as live settings.
        setPicker(limitToActiveHours = false)

        composeRule.onNodeWithText(string(R.string.limit_to_active_hours)).assertIsOff()
        composeRule.onNodeWithText(string(R.string.time_window_start)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.time_window_end)).assertIsNotEnabled()
        composeRule.onNodeWithText("09:00").assertExists()
        composeRule.onNodeWithText("17:00").assertExists()
    }

    @Test
    fun untickingTheCheckboxDropsTheHours() {
        var limit: Boolean? = null
        setPicker(limitToActiveHours = true, onLimitToActiveHoursChange = { limit = it })

        composeRule.onNodeWithText(string(R.string.limit_to_active_hours)).performClick()

        assertEquals(false, limit)
    }

    @Test
    fun tickingTheCheckboxRestoresTheHours() {
        var limit: Boolean? = null
        setPicker(limitToActiveHours = false, onLimitToActiveHoursChange = { limit = it })

        composeRule.onNodeWithText(string(R.string.limit_to_active_hours)).performClick()

        assertEquals(true, limit)
    }
}

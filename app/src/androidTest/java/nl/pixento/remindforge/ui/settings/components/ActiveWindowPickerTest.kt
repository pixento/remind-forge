package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import java.time.LocalTime
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActiveWindowPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun string(resId: Int) = context.getString(resId)

    private fun setPicker(
        activeWindowMode: ActiveWindowMode,
        onActiveWindowModeChange: (ActiveWindowMode) -> Unit = {},
        onWindowStartChange: (LocalTime) -> Unit = {},
        onOpenDoNotDisturbSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            // The picker emits a run of sibling rows, so it needs the same Column its SettingsGroup
            // gives it on the real screen. Stack them instead and every row overlaps, leaving the
            // last one to swallow clicks meant for the others.
            SettingsGroup {
                ActiveWindowPicker(
                    activeWindowMode = activeWindowMode,
                    windowStart = LocalTime.of(9, 0),
                    windowEnd = LocalTime.of(17, 0),
                    onActiveWindowModeChange = onActiveWindowModeChange,
                    onWindowStartChange = onWindowStartChange,
                    onWindowEndChange = {},
                    onOpenDoNotDisturbSettings = onOpenDoNotDisturbSettings,
                )
            }
        }
    }

    @Test
    fun customTimesLeavesTheTimeRowsLive() {
        setPicker(activeWindowMode = ActiveWindowMode.CUSTOM_TIMES)

        composeRule.onNodeWithText(string(R.string.follow_do_not_disturb_schedule)).assertIsOff()
        composeRule.onNodeWithText(string(R.string.time_window_start)).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.time_window_end)).assertIsEnabled()
    }

    @Test
    fun followingDoNotDisturbKeepsTheTimeRowsVisibleButDisabled() {
        // The times still show their stored values - unticking restores them - but they no longer
        // decide anything, so they must not read as live settings.
        setPicker(activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF)

        composeRule.onNodeWithText(string(R.string.follow_do_not_disturb_schedule)).assertIsOn()
        composeRule.onNodeWithText(string(R.string.time_window_start)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.time_window_end)).assertIsNotEnabled()
        composeRule.onNodeWithText("09:00").assertExists()
        composeRule.onNodeWithText("17:00").assertExists()
    }

    @Test
    fun tickingTheCheckboxSwitchesToFollowingDoNotDisturb() {
        var selected: ActiveWindowMode? = null
        setPicker(
            activeWindowMode = ActiveWindowMode.CUSTOM_TIMES,
            onActiveWindowModeChange = { selected = it },
        )

        composeRule.onNodeWithText(string(R.string.follow_do_not_disturb_schedule)).performClick()

        assertEquals(ActiveWindowMode.DO_NOT_DISTURB_OFF, selected)
    }

    @Test
    fun untickingTheCheckboxSwitchesBackToCustomTimes() {
        var selected: ActiveWindowMode? = null
        setPicker(
            activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF,
            onActiveWindowModeChange = { selected = it },
        )

        composeRule.onNodeWithText(string(R.string.follow_do_not_disturb_schedule)).performClick()

        assertEquals(ActiveWindowMode.CUSTOM_TIMES, selected)
    }

    @Test
    fun aDisabledTimeRowDoesNotOpenItsPicker() {
        var changed = false
        setPicker(
            activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF,
            onWindowStartChange = { changed = true },
        )

        composeRule.onNodeWithText(string(R.string.time_window_start)).performClick()

        assertEquals(false, changed)
    }

    @Test
    fun theDoNotDisturbSettingsShortcutStaysLiveOnCustomTimes() {
        // It's how you go and look at your Do Not Disturb schedule before deciding whether to
        // follow it, so it can't only work once you already have.
        var opened = 0
        setPicker(
            activeWindowMode = ActiveWindowMode.CUSTOM_TIMES,
            onOpenDoNotDisturbSettings = { opened++ },
        )

        composeRule.onNodeWithText(string(R.string.do_not_disturb_settings_title))
            .assertIsEnabled()
            .performClick()

        assertEquals(1, opened)
    }

    @Test
    fun theDoNotDisturbSettingsShortcutStaysLiveWhileFollowingDoNotDisturb() {
        var opened = 0
        setPicker(
            activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF,
            onOpenDoNotDisturbSettings = { opened++ },
        )

        composeRule.onNodeWithText(string(R.string.do_not_disturb_settings_title))
            .assertIsEnabled()
            .performClick()

        assertEquals(1, opened)
    }
}

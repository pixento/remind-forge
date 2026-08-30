package nl.pixento.betterhabits.ui.settings.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import nl.pixento.betterhabits.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AutoPausePickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun string(resId: Int) = context.getString(resId)

    private fun setPicker(
        pauseDuringDoNotDisturb: Boolean = false,
        pauseDuringAndroidAuto: Boolean = false,
        onPauseDuringDoNotDisturbChange: (Boolean) -> Unit = {},
        onPauseDuringAndroidAutoChange: (Boolean) -> Unit = {},
        onOpenDoNotDisturbSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            // A run of sibling rows, so it needs the same Column its SettingsGroup gives it on the
            // real screen - stacked instead, the last row swallows every click.
            SettingsGroup {
                AutoPausePicker(
                    pauseDuringDoNotDisturb = pauseDuringDoNotDisturb,
                    pauseDuringAndroidAuto = pauseDuringAndroidAuto,
                    onPauseDuringDoNotDisturbChange = onPauseDuringDoNotDisturbChange,
                    onPauseDuringAndroidAutoChange = onPauseDuringAndroidAutoChange,
                    onOpenDoNotDisturbSettings = onOpenDoNotDisturbSettings,
                )
            }
        }
    }

    @Test
    fun eachConditionShowsItsOwnState() {
        // The whole point of the arrangement: they are independent, not a single choice.
        setPicker(pauseDuringDoNotDisturb = true, pauseDuringAndroidAuto = false)

        composeRule.onNodeWithText(string(R.string.pause_during_do_not_disturb)).assertIsOn()
        composeRule.onNodeWithText(string(R.string.pause_during_android_auto)).assertIsOff()
    }

    @Test
    fun tickingOneConditionLeavesTheOtherAlone() {
        var doNotDisturb: Boolean? = null
        var androidAuto: Boolean? = null
        setPicker(
            onPauseDuringDoNotDisturbChange = { doNotDisturb = it },
            onPauseDuringAndroidAutoChange = { androidAuto = it },
        )

        composeRule.onNodeWithText(string(R.string.pause_during_android_auto)).performClick()

        assertEquals(true, androidAuto)
        assertEquals(null, doNotDisturb)
    }

    @Test
    fun untickingAConditionReportsItOff() {
        var doNotDisturb: Boolean? = null
        setPicker(
            pauseDuringDoNotDisturb = true,
            onPauseDuringDoNotDisturbChange = { doNotDisturb = it },
        )

        composeRule.onNodeWithText(string(R.string.pause_during_do_not_disturb)).performClick()

        assertEquals(false, doNotDisturb)
    }

    @Test
    fun theDoNotDisturbSettingsShortcutStaysLiveWhicheverWayTheCheckboxIsSet() {
        // It's how you go and look at your Do Not Disturb schedule before deciding whether to
        // follow it, so it can't only work once you already have.
        var opened = 0
        setPicker(pauseDuringDoNotDisturb = false, onOpenDoNotDisturbSettings = { opened++ })

        composeRule.onNodeWithText(string(R.string.do_not_disturb_settings_title))
            .assertIsEnabled()
            .performClick()

        assertEquals(1, opened)
    }
}

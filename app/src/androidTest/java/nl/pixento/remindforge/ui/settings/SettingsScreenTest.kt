package nl.pixento.remindforge.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        uiState: SettingsUiState = SettingsUiState(),
        onEnabledChanged: (Boolean) -> Unit = {},
        onRequestExactAlarmPermission: () -> Unit = {},
    ) {
        composeRule.setContent {
            SettingsScreen(
                uiState = uiState,
                onEnabledChanged = onEnabledChanged,
                onIntervalChanged = {},
                onWindowStartChanged = {},
                onWindowEndChanged = {},
                onVibrationPatternSelected = {},
                onRingtoneSelected = {},
                onRequestExactAlarmPermission = onRequestExactAlarmPermission,
            )
        }
    }

    @Test
    fun permissionBannerHiddenWhenNotNeeded() {
        setScreen(uiState = SettingsUiState(needsExactAlarmPermission = false))
        composeRule.onNodeWithText("Grant permission").assertDoesNotExist()
    }

    @Test
    fun permissionBannerShownWhenNeeded() {
        setScreen(uiState = SettingsUiState(needsExactAlarmPermission = true))
        composeRule.onNodeWithText("Grant permission").assertExists()
    }

    @Test
    fun grantPermissionButtonInvokesCallback() {
        var requested = false
        setScreen(
            uiState = SettingsUiState(needsExactAlarmPermission = true),
            onRequestExactAlarmPermission = { requested = true },
        )
        composeRule.onNodeWithText("Grant permission").performClick()
        assertTrue(requested)
    }

    @Test
    fun enabledSwitchReflectsState() {
        setScreen(uiState = SettingsUiState(enabled = true))
        composeRule.onNodeWithTag("enabledSwitch").assertIsOn()
    }

    @Test
    fun togglingEnabledSwitchInvokesCallback() {
        var enabled: Boolean? = null
        setScreen(
            uiState = SettingsUiState(enabled = false),
            onEnabledChanged = { enabled = it },
        )
        composeRule.onNodeWithTag("enabledSwitch").assertIsOff().performClick()
        assertTrue(enabled == true)
    }

    @Test
    fun bothAlertChannelsAreAlwaysShownWithTheirCurrentValue() {
        setScreen(
            uiState = SettingsUiState(
                vibrationPattern = VibrationPatternType.DOUBLE_PULSE,
                ringtoneUri = "content://media/ringtone/1",
                ringtoneTitle = "Galaxy Bells",
            ),
        )

        composeRule.onNodeWithText("Vibration").assertExists()
        composeRule.onNodeWithText(VibrationPatternType.DOUBLE_PULSE.label).assertExists()
        composeRule.onNodeWithText("Sound").assertExists()
        composeRule.onNodeWithText("Galaxy Bells").assertExists()
    }

    @Test
    fun soundRowReadsSilentWithNoRingtoneSelected() {
        setScreen(uiState = SettingsUiState(ringtoneUri = null, ringtoneTitle = null))
        composeRule.onNodeWithText("Silent").assertExists()
    }

    @Test
    fun warningHiddenWhileEitherChannelStillAlerts() {
        setScreen(
            uiState = SettingsUiState(
                vibrationPattern = VibrationPatternType.SILENT,
                ringtoneUri = "content://media/ringtone/1",
                ringtoneTitle = "Galaxy Bells",
            ),
        )
        composeRule.onNodeWithText(NO_ALERT_WARNING).assertDoesNotExist()
    }

    @Test
    fun warningShownWhenBothChannelsAreSilent() {
        setScreen(
            uiState = SettingsUiState(
                vibrationPattern = VibrationPatternType.SILENT,
                ringtoneUri = null,
            ),
        )
        composeRule.onNodeWithText(NO_ALERT_WARNING).assertExists()
    }

    private companion object {
        const val NO_ALERT_WARNING =
            "No vibration or sound selected - reminders will fire silently."
    }
}

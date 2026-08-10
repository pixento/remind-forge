package nl.pixento.remindforge.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import nl.pixento.remindforge.domain.model.AlertMode
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
                onAlertModeChanged = {},
                onVibrationPatternSelected = {},
                onPreviewVibration = {},
                onRingtoneSelected = {},
                onPreviewRingtone = {},
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
    fun vibrationSectionShownForVibrationMode() {
        setScreen(uiState = SettingsUiState(alertMode = AlertMode.VIBRATION))
        composeRule.onNodeWithText("Short pulse").assertExists()
    }

    @Test
    fun ringtoneSectionShownForRingtoneMode() {
        setScreen(uiState = SettingsUiState(alertMode = AlertMode.RINGTONE))
        composeRule.onNodeWithText("Choose ringtone").assertExists()
        composeRule.onNodeWithText("Short pulse").assertDoesNotExist()
    }
}

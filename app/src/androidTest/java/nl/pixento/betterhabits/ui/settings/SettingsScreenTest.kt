package nl.pixento.betterhabits.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import nl.pixento.betterhabits.R
import nl.pixento.betterhabits.domain.model.VibrationPatternType
import nl.pixento.betterhabits.ui.settings.vibration.labelRes
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun string(resId: Int, vararg formatArgs: Any) = context.getString(resId, *formatArgs)

    /**
     * The raw next-reminder template ("Next reminder around %1$s") with the placeholder stripped,
     * so a substring match stays valid regardless of where the locale puts the placeholder.
     */
    private val nextReminderPrefix =
        context.getString(R.string.next_reminder_around).substringBefore("%").trim()

    private fun setScreen(
        uiState: SettingsUiState = SettingsUiState(),
        onEnabledChanged: (Boolean) -> Unit = {},
        onRequestExactAlarmPermission: () -> Unit = {},
    ) {
        composeRule.setContent {
            // The screen is a LazyColumn, so an item below the fold is never composed and an
            // assertDoesNotExist on it would pass for the wrong reason (and an assertExists fail
            // for the wrong reason) depending only on the device's screen height. Give it a
            // viewport taller than the content so presence and absence both mean what they say.
            Box(modifier = Modifier.requiredHeight(2000.dp)) {
                SettingsScreen(
                    uiState = uiState,
                    onEnabledChanged = onEnabledChanged,
                    onIntervalChanged = { _, _ -> },
                    onLimitToActiveHoursChanged = {},
                    onWindowStartChanged = {},
                    onWindowEndChanged = {},
                    onVibrationPatternSelected = {},
                    onRingtoneSelected = {},
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                )
            }
        }
    }

    @Test
    fun permissionBannerHiddenWhenNotNeeded() {
        setScreen(uiState = SettingsUiState(needsExactAlarmPermission = false))
        composeRule.onNodeWithText(string(R.string.exact_alarm_banner_action)).assertDoesNotExist()
    }

    @Test
    fun grantPermissionButtonInvokesCallback() {
        var requested = false
        setScreen(
            uiState = SettingsUiState(needsExactAlarmPermission = true),
            onRequestExactAlarmPermission = { requested = true },
        )
        composeRule.onNodeWithText(string(R.string.exact_alarm_banner_action)).performClick()
        assertTrue(requested)
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

        composeRule.onNodeWithText(string(R.string.vibration_label)).assertExists()
        composeRule.onNodeWithText(string(VibrationPatternType.DOUBLE_PULSE.labelRes())).assertExists()
        composeRule.onNodeWithText(string(R.string.sound_label)).assertExists()
        composeRule.onNodeWithText("Galaxy Bells").assertExists()
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
        composeRule.onNodeWithText(string(R.string.no_alert_warning)).assertDoesNotExist()
    }

    @Test
    fun warningShownWhenBothChannelsAreSilent() {
        setScreen(
            uiState = SettingsUiState(
                vibrationPattern = VibrationPatternType.SILENT,
                ringtoneUri = null,
            ),
        )
        composeRule.onNodeWithText(string(R.string.no_alert_warning)).assertExists()
    }

    @Test
    fun pausedTextReplacesNextReminderWhilePausingForDoNotDisturbAndItIsOn() {
        // The alarm is genuinely pending, but every tick skips its alert - so the row has to say
        // that instead of promising a reminder at a time nothing will happen at.
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                pauseDuringDoNotDisturb = true,
                doNotDisturbActive = true,
                nextTriggerAtMillis = Instant.now().plusSeconds(600).toEpochMilli(),
            ),
        )
        composeRule.onNodeWithText(string(R.string.paused_for_do_not_disturb)).assertExists()
        composeRule.onNodeWithText(nextReminderPrefix, substring = true).assertDoesNotExist()
    }

    @Test
    fun pausedTextReplacesNextReminderWhilePausingForAndroidAutoAndItIsConnected() {
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                pauseDuringAndroidAuto = true,
                androidAutoConnected = true,
                nextTriggerAtMillis = Instant.now().plusSeconds(600).toEpochMilli(),
            ),
        )
        composeRule.onNodeWithText(string(R.string.paused_for_android_auto)).assertExists()
        composeRule.onNodeWithText(nextReminderPrefix, substring = true).assertDoesNotExist()
    }

    @Test
    fun nextReminderStillShownWhenTheConditionHoldsButWasNotOptedInto() {
        // Both conditions are live on the device and neither was asked for, so nothing is paused.
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                doNotDisturbActive = true,
                androidAutoConnected = true,
                nextTriggerAtMillis = Instant.now().plusSeconds(600).toEpochMilli(),
            ),
        )
        composeRule.onNodeWithText(string(R.string.paused_for_do_not_disturb)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.paused_for_android_auto)).assertDoesNotExist()
        composeRule.onNodeWithText(nextReminderPrefix, substring = true).assertExists()
    }

    @Test
    fun doNotDisturbWinsTheStatusLineWhenBothConditionsPause() {
        // One line reports the chain's state, so the reasons need the order
        // TriggerReminderUseCase resolves them in.
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                pauseDuringDoNotDisturb = true,
                pauseDuringAndroidAuto = true,
                doNotDisturbActive = true,
                androidAutoConnected = true,
                nextTriggerAtMillis = Instant.now().plusSeconds(600).toEpochMilli(),
            ),
        )
        composeRule.onNodeWithText(string(R.string.paused_for_do_not_disturb)).assertExists()
        composeRule.onNodeWithText(string(R.string.paused_for_android_auto)).assertDoesNotExist()
    }

    @Test
    fun nextReminderShowsThePendingAlarmTime() {
        val trigger = Instant.now().plusSeconds(600)
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                nextTriggerAtMillis = trigger.toEpochMilli(),
            ),
        )

        val expected = DateTimeFormatter.ofPattern("HH:mm")
            .format(trigger.atZone(ZoneId.systemDefault()))
        composeRule.onNodeWithText(string(R.string.next_reminder_around, expected)).assertExists()
    }

    @Test
    fun nextReminderHiddenWhenNothingIsScheduled() {
        setScreen(uiState = SettingsUiState(enabled = true, nextTriggerAtMillis = null))
        composeRule.onNodeWithText(nextReminderPrefix, substring = true).assertDoesNotExist()
    }
}

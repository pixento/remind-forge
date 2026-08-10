package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import nl.pixento.remindforge.domain.model.AlertMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlertModeSelectorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingRingtoneInvokesCallback() {
        var selected: AlertMode? = null
        composeRule.setContent {
            AlertModeSelector(selected = AlertMode.VIBRATION, onSelect = { selected = it })
        }
        composeRule.onNodeWithText("Ringtone").performClick()
        assertEquals(AlertMode.RINGTONE, selected)
    }

    @Test
    fun selectingVibrationInvokesCallback() {
        var selected: AlertMode? = null
        composeRule.setContent {
            AlertModeSelector(selected = AlertMode.RINGTONE, onSelect = { selected = it })
        }
        composeRule.onNodeWithText("Vibration").performClick()
        assertEquals(AlertMode.VIBRATION, selected)
    }
}

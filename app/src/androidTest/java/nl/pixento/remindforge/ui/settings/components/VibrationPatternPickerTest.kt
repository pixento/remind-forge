package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class VibrationPatternPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingAPatternInvokesOnSelect() {
        var selected: VibrationPatternType? = null
        composeRule.setContent {
            VibrationPatternPicker(
                selected = VibrationPatternType.SHORT_PULSE,
                onSelect = { selected = it },
                onPreview = {},
            )
        }
        composeRule.onNodeWithText(VibrationPatternType.LONG_PULSE.label).performClick()
        assertEquals(VibrationPatternType.LONG_PULSE, selected)
    }

    @Test
    fun previewButtonInvokesOnPreviewWithoutChangingSelection() {
        var selected: VibrationPatternType? = null
        var previewed: VibrationPatternType? = null
        composeRule.setContent {
            VibrationPatternPicker(
                selected = VibrationPatternType.SHORT_PULSE,
                onSelect = { selected = it },
                onPreview = { previewed = it },
            )
        }
        // Rows render in VibrationPatternType.entries order; index 1 is LONG_PULSE's preview button.
        composeRule.onAllNodesWithText("Preview")[1].performClick()
        assertEquals(VibrationPatternType.LONG_PULSE, previewed)
        assertNull(selected)
    }
}

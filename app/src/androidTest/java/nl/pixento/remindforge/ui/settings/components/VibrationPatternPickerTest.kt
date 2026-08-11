package nl.pixento.remindforge.ui.settings.components

import androidx.compose.ui.test.junit4.createComposeRule
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

    private var selected: VibrationPatternType? = null
    private var previewed: VibrationPatternType? = null

    private fun setPicker(current: VibrationPatternType = VibrationPatternType.SHORT_PULSE) {
        composeRule.setContent {
            VibrationPatternPicker(
                selected = current,
                onSelect = { selected = it },
                onPreview = { previewed = it },
            )
        }
    }

    @Test
    fun rowShowsTheSelectedPatternAndOpensTheDialogWithoutSelecting() {
        setPicker(current = VibrationPatternType.DOUBLE_PULSE)

        composeRule.onNodeWithText(VibrationPatternType.DOUBLE_PULSE.label).performClick()

        composeRule.onNodeWithText("Vibration pattern").assertExists()
        assertNull(selected)
    }

    @Test
    fun tappingAPatternPreviewsItButDoesNotCommitUntilOk() {
        setPicker()
        composeRule.onNodeWithText(VibrationPatternType.SHORT_PULSE.label).performClick()

        composeRule.onNodeWithText(VibrationPatternType.LONG_PULSE.label).performClick()

        assertEquals(VibrationPatternType.LONG_PULSE, previewed)
        assertNull(selected)
    }

    @Test
    fun okCommitsTheTappedPattern() {
        setPicker()
        composeRule.onNodeWithText(VibrationPatternType.SHORT_PULSE.label).performClick()
        composeRule.onNodeWithText(VibrationPatternType.LONG_PULSE.label).performClick()

        composeRule.onNodeWithText("OK").performClick()

        assertEquals(VibrationPatternType.LONG_PULSE, selected)
        composeRule.onNodeWithText("Vibration pattern").assertDoesNotExist()
    }

    @Test
    fun cancelDiscardsTheTappedPattern() {
        setPicker()
        composeRule.onNodeWithText(VibrationPatternType.SHORT_PULSE.label).performClick()
        composeRule.onNodeWithText(VibrationPatternType.LONG_PULSE.label).performClick()

        composeRule.onNodeWithText("Cancel").performClick()

        assertNull(selected)
        composeRule.onNodeWithText("Vibration pattern").assertDoesNotExist()
    }

    @Test
    fun silentIsOfferedAsAPattern() {
        setPicker()
        composeRule.onNodeWithText(VibrationPatternType.SHORT_PULSE.label).performClick()

        composeRule.onNodeWithText(VibrationPatternType.SILENT.label).performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(VibrationPatternType.SILENT, selected)
    }
}

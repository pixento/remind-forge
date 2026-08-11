package nl.pixento.remindforge.ui.settings.vibration

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VibrationPatternScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var selected: VibrationPatternType? = null
    private var backPressed = false

    private fun setScreen(current: VibrationPatternType = VibrationPatternType.SHORT_PULSE) {
        composeRule.setContent {
            VibrationPatternScreen(
                selected = current,
                onSelect = { selected = it },
                onBack = { backPressed = true },
            )
        }
    }

    @Test
    fun everyPatternIsOffered() {
        setScreen()

        VibrationPatternType.entries.forEach { pattern ->
            composeRule.onNodeWithText(pattern.label).performScrollTo().assertExists()
        }
    }

    @Test
    fun theCurrentPatternIsMarkedSelected() {
        setScreen(current = VibrationPatternType.DOUBLE_PULSE)

        composeRule.onNodeWithText(VibrationPatternType.DOUBLE_PULSE.label)
            .performScrollTo()
            .assertIsSelected()
    }

    @Test
    fun tappingAPatternCommitsItStraightAway() {
        setScreen()

        composeRule.onNodeWithText(VibrationPatternType.LONG_PULSE.label)
            .performScrollTo()
            .performClick()

        assertEquals(VibrationPatternType.LONG_PULSE, selected)
    }

    @Test
    fun silentCanBePicked() {
        setScreen()

        composeRule.onNodeWithText(VibrationPatternType.SILENT.label)
            .performScrollTo()
            .performClick()

        assertEquals(VibrationPatternType.SILENT, selected)
    }

    @Test
    fun theBackArrowLeavesWithoutPicking() {
        setScreen()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backPressed)
        assertNull(selected)
    }
}

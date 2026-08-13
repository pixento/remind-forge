package nl.pixento.remindforge.ui.settings.vibration

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VibrationPatternScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val VibrationPatternType.label: String
        get() = context.getString(labelRes())

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
    fun theBackArrowLeavesWithoutPicking() {
        setScreen()

        composeRule.onNodeWithContentDescription(context.getString(R.string.vibration_picker_back))
            .performClick()

        assertTrue(backPressed)
        assertNull(selected)
    }
}

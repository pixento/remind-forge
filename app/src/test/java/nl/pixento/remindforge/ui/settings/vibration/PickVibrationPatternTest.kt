package nl.pixento.remindforge.ui.settings.vibration

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PickVibrationPatternTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `intent targets the picker and carries the pattern to preselect`() {
        val intent = PickVibrationPattern.createIntent(context, VibrationPatternType.DOUBLE_PULSE)

        assertEquals(VibrationPickerActivity::class.java.name, intent.component?.className)
        assertEquals(
            VibrationPatternType.DOUBLE_PULSE.name,
            intent.getStringExtra(EXTRA_INITIAL_PATTERN),
        )
    }

    @Test
    fun `a picked pattern round-trips back to the caller`() {
        assertEquals(VibrationPatternType.SILENT, pickedResult(VibrationPatternType.SILENT.name))
        assertEquals(
            VibrationPatternType.LONG_SHORT_LONG,
            pickedResult(VibrationPatternType.LONG_SHORT_LONG.name),
        )
    }

    @Test
    fun `leaving the picker without choosing reports no change`() {
        assertNull(PickVibrationPattern.parseResult(Activity.RESULT_CANCELED, null))
    }

    @Test
    fun `a result without a pattern reports no change`() {
        assertNull(PickVibrationPattern.parseResult(Activity.RESULT_OK, Intent()))
    }

    @Test
    fun `an unknown pattern name reports no change instead of crashing`() {
        assertNull(pickedResult("MORSE_CODE"))
    }

    /** Mirrors the result the activity sets for a tapped pattern. */
    private fun pickedResult(name: String): VibrationPatternType? =
        PickVibrationPattern.parseResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_PICKED_PATTERN, name),
        )
}

package nl.pixento.remindforge.scheduling

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class ExactAlarmPermissionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Config(sdk = [30])
    @Test
    fun `pre-31 always reports permission granted`() {
        assertTrue(ExactAlarmPermission.canScheduleExactAlarms(context))
    }

    @Config(sdk = [33])
    @Test
    fun `33+ reflects shadow alarm manager grant state when granted`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertTrue(ExactAlarmPermission.canScheduleExactAlarms(context))
    }

    @Config(sdk = [33])
    @Test
    fun `33+ reflects shadow alarm manager grant state when denied`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(ExactAlarmPermission.canScheduleExactAlarms(context))
    }
}

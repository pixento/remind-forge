package nl.pixento.betterhabits.scheduling

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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

    @Test
    fun `the permission request opens the system screen scoped to this package`() {
        // A missing package uri drops the user on the full app list instead of this app's toggle.
        val intent = ExactAlarmPermission.buildRequestIntent(context)

        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, intent.action)
        assertEquals("package", intent.data?.scheme)
        assertEquals(context.packageName, intent.data?.schemeSpecificPart)
    }
}

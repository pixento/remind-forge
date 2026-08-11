package nl.pixento.remindforge.scheduling

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BatteryOptimizationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    @Test
    fun `an exempt app reports no battery restriction`() {
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, true)
        assertTrue(BatteryOptimization.isIgnoringBatteryOptimizations(context))
    }

    @Test
    fun `a restricted app is reported so the screen can nudge the user`() {
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        assertFalse(BatteryOptimization.isIgnoringBatteryOptimizations(context))
    }

    @Test
    fun `a device without a PowerManager fails open rather than nagging`() {
        // Robolectric always supplies one, so the ?: true branch needs a context that doesn't.
        val serviceless = mockk<Context>()
        every { serviceless.getSystemService(Context.POWER_SERVICE) } returns null

        assertTrue(BatteryOptimization.isIgnoringBatteryOptimizations(serviceless))
    }

    @Test
    fun `the exemption request opens the system screen scoped to this package`() {
        // A missing package uri drops the user on the full app list instead of this app's toggle.
        val intent = BatteryOptimization.buildRequestIntent(context)

        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertEquals("package", intent.data?.scheme)
        assertEquals(context.packageName, intent.data?.schemeSpecificPart)
    }
}

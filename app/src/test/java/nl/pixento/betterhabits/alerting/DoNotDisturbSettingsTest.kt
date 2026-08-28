package nl.pixento.betterhabits.alerting

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Which of these actions a device handles varies, so the fallback order is the behaviour under
 * test: the schedules screen first, the public priority screen next, sound settings as the
 * always-present last resort.
 */
@RunWith(RobolectricTestRunner::class)
class DoNotDisturbSettingsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun makeResolvable(action: String) {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "Settings\$${action.substringAfterLast('.')}Activity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(Intent(action), resolveInfo)
    }

    @Test
    fun `the zen mode screen wins when the device has one`() {
        makeResolvable("android.settings.ZEN_MODE_SETTINGS")
        makeResolvable(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)

        assertEquals(
            "android.settings.ZEN_MODE_SETTINGS",
            DoNotDisturbSettings.buildRequestIntent(context).action,
        )
    }

    @Test
    fun `the priority screen is used when the zen mode screen is missing`() {
        makeResolvable(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)

        assertEquals(
            Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS,
            DoNotDisturbSettings.buildRequestIntent(context).action,
        )
    }

    @Test
    fun `sound settings is the fallback when no Do Not Disturb screen resolves`() {
        assertEquals(
            Settings.ACTION_SOUND_SETTINGS,
            DoNotDisturbSettings.buildRequestIntent(context).action,
        )
    }

    @Test
    fun `the intent is implicit so any settings app can handle it`() {
        makeResolvable("android.settings.ZEN_MODE_SETTINGS")

        val intent = DoNotDisturbSettings.buildRequestIntent(context)

        assertEquals(null, intent.component as ComponentName?)
        assertEquals(null, intent.`package`)
    }
}

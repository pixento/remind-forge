package nl.pixento.betterhabits.alerting

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidDoNotDisturbMonitorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun monitorWithFilter(filter: Int): AndroidDoNotDisturbMonitor {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Setting the filter for real needs Do-Not-Disturb access; the shadow just records it.
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)
        notificationManager.setInterruptionFilter(filter)
        return AndroidDoNotDisturbMonitor(context)
    }

    @Test
    fun `the normal filter is not Do Not Disturb`() {
        assertFalse(
            monitorWithFilter(NotificationManager.INTERRUPTION_FILTER_ALL).isDoNotDisturbActive(),
        )
    }

    @Test
    fun `priority and none count as Do Not Disturb`() {
        assertTrue(
            monitorWithFilter(
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            ).isDoNotDisturbActive(),
        )
        assertTrue(
            monitorWithFilter(NotificationManager.INTERRUPTION_FILTER_NONE).isDoNotDisturbActive(),
        )
    }

    @Test
    fun `alarms-only counts as Do Not Disturb even though it would let our alerts through`() {
        // AndroidAlertPlayer classes both channels as USAGE_ALARM, so the OS wouldn't silence us
        // here - but a user who asked to follow Do Not Disturb means the plain reading of it.
        assertTrue(
            monitorWithFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS).isDoNotDisturbActive(),
        )
    }

    @Test
    fun `an unknown filter fails open rather than silencing reminders forever`() {
        assertFalse(
            monitorWithFilter(
                NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
            ).isDoNotDisturbActive(),
        )
    }

    @Test
    fun `a missing notification service fails open too`() {
        val brokenContext = mockk<Context>()
        every { brokenContext.applicationContext } returns brokenContext
        every { brokenContext.getSystemService(NotificationManager::class.java) } returns null

        assertFalse(AndroidDoNotDisturbMonitor(brokenContext).isDoNotDisturbActive())
    }
}

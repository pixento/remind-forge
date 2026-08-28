package nl.pixento.betterhabits.receivers

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import nl.pixento.betterhabits.BetterHabitsApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BootCompletedReceiverTest {

    private val app: BetterHabitsApplication = ApplicationProvider.getApplicationContext()
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun enableAlwaysActive() = runBlocking {
        app.container.settingsRepository.setEnabled(true)
        app.container.settingsRepository.setWindowStart(LocalTime.MIN)
        app.container.settingsRepository.setWindowEnd(LocalTime.MIN)
    }

    @Test
    fun `BOOT_COMPLETED reschedules when enabled`() {
        enableAlwaysActive()

        BootCompletedReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertNotNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    @Test
    fun `MY_PACKAGE_REPLACED also reschedules when enabled`() {
        enableAlwaysActive()

        BootCompletedReceiver().onReceive(app, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        assertNotNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    @Test
    fun `QUICKBOOT_POWERON also reschedules when enabled`() {
        // Some OEM ROMs deliver this instead of BOOT_COMPLETED after a fast boot; without it the
        // chain would stay dead on those devices until the app is opened.
        enableAlwaysActive()

        BootCompletedReceiver().onReceive(app, Intent("android.intent.action.QUICKBOOT_POWERON"))

        assertNotNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    @Test
    fun `does not reschedule when disabled`() {
        runBlocking { app.container.settingsRepository.setEnabled(false) }

        BootCompletedReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    @Test
    fun `ignores unrelated and missing actions`() {
        enableAlwaysActive()

        BootCompletedReceiver().onReceive(app, Intent("some.other.action"))
        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())

        BootCompletedReceiver().onReceive(app, Intent())
        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }
}

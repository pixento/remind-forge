package nl.pixento.remindforge.receivers

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import nl.pixento.remindforge.RemindForgeApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReminderAlarmReceiverTest {

    private val app: RemindForgeApplication = ApplicationProvider.getApplicationContext()
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Test
    fun `enabled settings within window reschedules from the scheduled-time extra`() {
        val scheduledAt = 1_780_000_000_000L // fixed reference instant used by the alarm chain

        runBlocking {
            app.container.settingsRepository.setEnabled(true)
            app.container.settingsRepository.setIntervalMinutes(10)
            app.container.settingsRepository.setWindowStart(LocalTime.MIN)
            app.container.settingsRepository.setWindowEnd(LocalTime.MIN) // always-active window
        }

        val intent = Intent(app, ReminderAlarmReceiver::class.java)
            .putExtra(ReminderAlarmReceiver.EXTRA_SCHEDULED_AT_MILLIS, scheduledAt)
        ReminderAlarmReceiver().onReceive(app, intent)

        val scheduled = shadowOf(alarmManager).nextScheduledAlarm!!
        assertEquals(scheduledAt + 10 * 60 * 1000L, scheduled.triggerAtTime)
    }

    @Test
    fun `missing scheduled-at extra is ignored`() {
        val intent = Intent(app, ReminderAlarmReceiver::class.java)
        ReminderAlarmReceiver().onReceive(app, intent)

        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }

    @Test
    fun `disabled settings do not reschedule`() {
        runBlocking { app.container.settingsRepository.setEnabled(false) }

        val intent = Intent(app, ReminderAlarmReceiver::class.java)
            .putExtra(ReminderAlarmReceiver.EXTRA_SCHEDULED_AT_MILLIS, 1_780_000_000_000L)
        ReminderAlarmReceiver().onReceive(app, intent)

        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }
}

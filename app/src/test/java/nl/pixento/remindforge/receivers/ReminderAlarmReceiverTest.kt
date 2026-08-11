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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReminderAlarmReceiverTest {

    private val app: RemindForgeApplication = ApplicationProvider.getApplicationContext()
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun enableAlwaysActiveChain(intervalMinutes: Int) {
        runBlocking {
            app.container.settingsRepository.setEnabled(true)
            app.container.settingsRepository.setIntervalMinutes(intervalMinutes)
            app.container.settingsRepository.setWindowStart(LocalTime.MIN)
            app.container.settingsRepository.setWindowEnd(LocalTime.MIN) // always-active window
        }
    }

    private fun fireTick(scheduledAtMillis: Long) {
        val intent = Intent(app, ReminderAlarmReceiver::class.java)
            .putExtra(ReminderAlarmReceiver.EXTRA_SCHEDULED_AT_MILLIS, scheduledAtMillis)
        ReminderAlarmReceiver().onReceive(app, intent)
    }

    @Test
    fun `enabled settings within window reschedules from the scheduled-time extra`() {
        enableAlwaysActiveChain(intervalMinutes = 10)
        // On time: the tick is handled well within its own interval, so the next slot is simply
        // one interval past the reference carried in the extra.
        val scheduledAt = System.currentTimeMillis()

        fireTick(scheduledAt)

        val scheduled = shadowOf(alarmManager).peekNextScheduledAlarm()!!
        assertEquals(scheduledAt + INTERVAL_MILLIS, scheduled.triggerAtMs)
    }

    @Test
    fun `a tick delivered long after its slot reschedules ahead of now, still on cadence`() {
        enableAlwaysActiveChain(intervalMinutes = 10)
        // A doze-deferred (or process-death-replayed) tick whose reference is hours in the past:
        // scheduling reference + one interval would fire instantly and replay every missed tick.
        val scheduledAt = System.currentTimeMillis() - 3 * 60 * 60 * 1000L

        fireTick(scheduledAt)

        val triggerAt = shadowOf(alarmManager).peekNextScheduledAlarm()!!.triggerAtMs
        assertTrue("expected a future trigger, got $triggerAt", triggerAt > System.currentTimeMillis())
        assertEquals(
            "expected the original cadence to be preserved",
            0L,
            (triggerAt - scheduledAt) % INTERVAL_MILLIS,
        )
    }

    @Test
    fun `missing scheduled-at extra is ignored`() {
        val intent = Intent(app, ReminderAlarmReceiver::class.java)
        ReminderAlarmReceiver().onReceive(app, intent)

        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    @Test
    fun `disabled settings do not reschedule`() {
        runBlocking { app.container.settingsRepository.setEnabled(false) }

        fireTick(1_780_000_000_000L)

        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    private companion object {
        private const val INTERVAL_MILLIS = 10 * 60 * 1000L
    }
}

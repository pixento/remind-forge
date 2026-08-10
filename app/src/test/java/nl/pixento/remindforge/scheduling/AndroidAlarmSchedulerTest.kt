package nl.pixento.remindforge.scheduling

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidAlarmSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scheduler = AndroidAlarmScheduler(context)

    @Test
    fun `scheduleNext registers an exact-and-allow-while-idle alarm at the given time`() {
        val triggerAt = 1_700_000_000_000L
        scheduler.scheduleNext(triggerAt)

        val scheduled = shadowOf(alarmManager).nextScheduledAlarm!!
        assertEquals(triggerAt, scheduled.triggerAtTime)
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled.type)
    }

    @Test
    fun `cancel clears the pending alarm`() {
        scheduler.scheduleNext(1_700_000_000_000L)
        scheduler.cancel()
        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }

    @Test
    fun `rescheduling replaces rather than duplicates the pending alarm`() {
        scheduler.scheduleNext(1_700_000_000_000L)
        scheduler.scheduleNext(1_700_000_600_000L)

        val shadowAlarmManager = shadowOf(alarmManager)
        val onlyAlarm = shadowAlarmManager.nextScheduledAlarm!!
        assertEquals(1_700_000_600_000L, onlyAlarm.triggerAtTime)
        assertNull(shadowAlarmManager.nextScheduledAlarm)
    }

    @Test
    fun `cancel without a prior schedule is a no-op`() {
        scheduler.cancel()
        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }
}

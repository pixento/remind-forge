package nl.pixento.remindforge.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import nl.pixento.remindforge.receivers.ReminderAlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class AndroidAlarmSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scheduler = AndroidAlarmScheduler(context)

    /** The trigger time the receiver will read back out of the pending broadcast. */
    @Suppress("DEPRECATION") // ScheduledAlarm.operation has no getter equivalent.
    private fun scheduledAtExtraOf(alarm: ShadowAlarmManager.ScheduledAlarm): Long =
        shadowOf(alarm.operation).savedIntent
            .getLongExtra(ReminderAlarmReceiver.EXTRA_SCHEDULED_AT_MILLIS, 0L)

    @Test
    fun `scheduleNext registers an exact-and-allow-while-idle alarm at the given time`() {
        val triggerAt = 1_700_000_000_000L
        scheduler.scheduleNext(triggerAt)

        val scheduled = shadowOf(alarmManager).peekNextScheduledAlarm()!!
        assertEquals(triggerAt, scheduled.triggerAtMs)
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled.getType())
        // Without allow-while-idle the chain stalls for the whole doze window.
        assertTrue(scheduled.isAllowWhileIdle)
    }

    @Test
    fun `the scheduled alarm carries its own trigger time for the receiver to chain from`() {
        // ReminderAlarmReceiver refuses to reschedule without this extra, so losing it would kill
        // the chain silently after one tick.
        val triggerAt = 1_700_000_000_000L
        scheduler.scheduleNext(triggerAt)

        val scheduled = shadowOf(alarmManager).peekNextScheduledAlarm()!!
        assertEquals(triggerAt, scheduledAtExtraOf(scheduled))
    }

    @Test
    fun `rescheduling refreshes the carried trigger time rather than keeping the stale one`() {
        // The chain reuses one PendingIntent under a fixed request code, so the extra only moves
        // with FLAG_UPDATE_CURRENT; a stale extra would peg the chain to its first tick forever.
        scheduler.scheduleNext(1_700_000_000_000L)
        scheduler.scheduleNext(1_700_000_600_000L)

        val scheduled = shadowOf(alarmManager).peekNextScheduledAlarm()!!
        assertEquals(1_700_000_600_000L, scheduledAtExtraOf(scheduled))
    }

    @Test
    fun `cancel clears the pending alarm`() {
        scheduler.scheduleNext(1_700_000_000_000L)
        scheduler.cancel()
        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    @Test
    fun `rescheduling replaces rather than duplicates the pending alarm`() {
        scheduler.scheduleNext(1_700_000_000_000L)
        scheduler.scheduleNext(1_700_000_600_000L)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        assertEquals(1_700_000_600_000L, shadowAlarmManager.peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun `cancel without a prior schedule is a no-op`() {
        scheduler.cancel()
        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }

    @Test
    fun `hasPending is false before any alarm is scheduled`() {
        assertEquals(false, scheduler.hasPending())
    }

    @Test
    fun `hasPending is true after scheduling`() {
        scheduler.scheduleNext(1_700_000_000_000L)
        assertEquals(true, scheduler.hasPending())
    }

    @Test
    fun `hasPending is false after cancel`() {
        scheduler.scheduleNext(1_700_000_000_000L)
        scheduler.cancel()
        assertEquals(false, scheduler.hasPending())
    }

    @Test
    @Config(shadows = [ThrowingExactAlarmShadow::class])
    fun `a denied exact-alarm permission falls back to an inexact alarm instead of killing the chain`() {
        // SCHEDULE_EXACT_ALARM can be revoked at any time from system settings. Losing precision is
        // acceptable; losing the alarm means the user never gets reminded again.
        val triggerAt = 1_700_000_000_000L

        scheduler.scheduleNext(triggerAt)

        val scheduled = shadowOf(alarmManager).peekNextScheduledAlarm()!!
        assertEquals(triggerAt, scheduled.triggerAtMs)
        assertEquals(triggerAt, scheduledAtExtraOf(scheduled))
        assertTrue(scheduler.hasPending())
    }
}

/**
 * Robolectric's own [ShadowAlarmManager] never throws, so the only way to exercise the revoked
 * permission path is to make the exact-alarm call fail the way the framework would.
 */
@Implements(AlarmManager::class)
class ThrowingExactAlarmShadow : ShadowAlarmManager() {

    @Implementation
    override fun setExactAndAllowWhileIdle(
        type: Int,
        triggerAtTime: Long,
        operation: PendingIntent?,
    ) {
        throw SecurityException("SCHEDULE_EXACT_ALARM not granted")
    }
}

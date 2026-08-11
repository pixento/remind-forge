package nl.pixento.remindforge.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import nl.pixento.remindforge.receivers.ReminderAlarmReceiver

/**
 * Wraps [AlarmManager.setExactAndAllowWhileIdle] behind a single stable [PendingIntent] so the
 * self-rescheduling alarm chain never has more than one alarm pending at a time. Any existing
 * alarm is explicitly cancelled before a new one is set, rather than relying on
 * FLAG_UPDATE_CURRENT replace semantics alone.
 */
class AndroidAlarmScheduler(
    private val context: Context,
) : AlarmScheduler {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleNext(triggerAtMillis: Long) {
        val pendingIntent = buildPendingIntent(triggerAtMillis)
        alarmManager.cancel(pendingIntent)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM not granted (default on API 33+ until the user opts in via
            // system settings) - keep the chain alive with a less precise, permission-free alarm
            // rather than letting it die silently.
            Log.w(
                TAG,
                "SCHEDULE_EXACT_ALARM denied; falling back to inexact alarm, which Doze may defer",
                e,
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    override fun cancel() {
        val pendingIntent = buildPendingIntent(scheduledAtMillis = null)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun hasPending(): Boolean {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) != null
    }

    private fun buildPendingIntent(scheduledAtMillis: Long?): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            if (scheduledAtMillis != null) {
                putExtra(ReminderAlarmReceiver.EXTRA_SCHEDULED_AT_MILLIS, scheduledAtMillis)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "AndroidAlarmScheduler"
        private const val REQUEST_CODE = 1001
    }
}

package nl.pixento.remindforge.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import nl.pixento.remindforge.RemindForgeApplication

/**
 * Fires when the self-rescheduling alarm chain ticks. Runs synchronously (not goAsync() + a
 * background coroutine) since the work here - one local DataStore read plus a Vibrator/
 * NotificationManager/AlarmManager call - is fast and well within the broadcast-receiver time
 * budget; this keeps the receiver trivially testable without coordinating with async completion.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.hasExtra(EXTRA_SCHEDULED_AT_MILLIS)) return
        val scheduledAtMillis = intent.getLongExtra(EXTRA_SCHEDULED_AT_MILLIS, 0L)

        val container = (context.applicationContext as RemindForgeApplication).container
        runBlocking {
            container.triggerReminderUseCase.onAlarmFired(scheduledAtMillis)
        }
    }

    companion object {
        const val EXTRA_SCHEDULED_AT_MILLIS = "extra_scheduled_at_millis"
    }
}

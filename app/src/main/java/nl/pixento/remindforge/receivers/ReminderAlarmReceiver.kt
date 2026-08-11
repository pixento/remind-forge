package nl.pixento.remindforge.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking
import nl.pixento.remindforge.RemindForgeApplication
import nl.pixento.remindforge.domain.AlarmFiredOutcome

/**
 * Fires when the self-rescheduling alarm chain ticks. Runs synchronously (not goAsync() + a
 * background coroutine) since the work here - one local DataStore read plus a Vibrator/
 * NotificationManager/AlarmManager call - is fast and well within the broadcast-receiver time
 * budget; this keeps the receiver trivially testable without coordinating with async completion.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.hasExtra(EXTRA_SCHEDULED_AT_MILLIS)) {
            Log.w(TAG, "onReceive: missing $EXTRA_SCHEDULED_AT_MILLIS extra, ignoring broadcast")
            return
        }
        val scheduledAtMillis = intent.getLongExtra(EXTRA_SCHEDULED_AT_MILLIS, 0L)
        Log.d(TAG, "tick: scheduledAtMillis=$scheduledAtMillis")

        val container = (context.applicationContext as RemindForgeApplication).container
        val outcome = runBlocking {
            container.triggerReminderUseCase.onAlarmFired(scheduledAtMillis)
        }
        if (outcome == AlarmFiredOutcome.FIRED) {
            Log.i(TAG, "tick: scheduledAtMillis=$scheduledAtMillis outcome=$outcome")
        } else {
            Log.w(TAG, "tick: scheduledAtMillis=$scheduledAtMillis outcome=$outcome")
        }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val EXTRA_SCHEDULED_AT_MILLIS = "extra_scheduled_at_millis"
    }
}

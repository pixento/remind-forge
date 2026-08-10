package nl.pixento.remindforge.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import nl.pixento.remindforge.RemindForgeApplication

/** AlarmManager alarms don't survive reboot; restart the chain from `now` if still enabled. */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val container = (context.applicationContext as RemindForgeApplication).container
        runBlocking {
            container.scheduleCoordinator.rescheduleFromNow()
        }
    }
}

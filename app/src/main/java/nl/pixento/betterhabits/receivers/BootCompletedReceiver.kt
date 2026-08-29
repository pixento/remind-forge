package nl.pixento.betterhabits.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import nl.pixento.betterhabits.BetterHabitsApplication

private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

/** AlarmManager alarms don't survive reboot; restart the chain from `now` if still enabled. */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != ACTION_QUICKBOOT_POWERON
        ) {
            return
        }

        val container = (context.applicationContext as BetterHabitsApplication).container
        runBlocking {
            container.scheduleCoordinator.rescheduleFromNow()
        }
    }
}

package nl.pixento.betterhabits.alerting

import android.app.NotificationManager
import android.content.Context

/**
 * Reads the global interruption filter. `getCurrentInterruptionFilter()` needs no permission and no
 * Do-Not-Disturb access grant (only *changing* the policy does), and has existed since API 23 - so
 * there's nothing to declare in the manifest and no SDK_INT gate below our minSdk 24.
 */
class AndroidDoNotDisturbMonitor(context: Context) : DoNotDisturbMonitor {

    private val appContext = context.applicationContext

    /**
     * Anything but [NotificationManager.INTERRUPTION_FILTER_ALL] counts as "be quiet", including
     * `INTERRUPTION_FILTER_ALARMS`: the OS would let our alerts through there, since
     * [AndroidAlertPlayer] classes them as alarms, but a user who asked to follow Do Not Disturb
     * means the plain reading of it.
     *
     * `INTERRUPTION_FILTER_UNKNOWN` is documented as "the value is unavailable for any reason", so
     * it's treated as *not* DND - failing open keeps reminders working on an odd build rather than
     * silently killing them, the same way [nl.pixento.betterhabits.scheduling.BatteryOptimization]
     * fails open.
     */
    override fun isDoNotDisturbActive(): Boolean {
        val notificationManager =
            appContext.getSystemService(NotificationManager::class.java) ?: return false
        return when (notificationManager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> false

            else -> true
        }
    }
}

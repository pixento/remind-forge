package nl.pixento.remindforge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import nl.pixento.remindforge.alerting.ReminderNotifier

class RemindForgeApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Silent by design: real alerting is done programmatically by AlertPlayer (Vibrator /
     * RingtoneManager directly), not by the channel, since a channel's sound/vibration can't be
     * changed after creation without deleting and recreating it.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            ReminderNotifier.CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

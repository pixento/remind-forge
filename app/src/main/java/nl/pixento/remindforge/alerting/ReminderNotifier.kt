package nl.pixento.remindforge.alerting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import nl.pixento.remindforge.R

/**
 * Posts a plain, silent tray entry for context. The channel itself carries no sound/vibration
 * (see [RemindForgeApplication]) — actual alerting is done programmatically by
 * [AlertPlayer], so this only needs to gate on POST_NOTIFICATIONS.
 */
class ReminderNotifier(
    private val context: Context,
) {
    fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        private const val NOTIFICATION_ID = 2001
    }
}

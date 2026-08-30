package nl.pixento.betterhabits.alerting

import android.content.Context
import android.net.Uri

/**
 * Reads the car-connection state Android Auto publishes, by querying the same content provider
 * `androidx.car.app.connection.CarConnection` wraps.
 *
 * `UiModeManager.getCurrentModeType()` cannot answer this: on Android 12+ projection no longer
 * changes the device's UI mode, and `UI_MODE_TYPE_CAR` belongs to the car's virtual display rather
 * than the phone's. The library itself is unusable here because it exposes the state only as
 * `LiveData` from an `@MainThread` constructor, while
 * [nl.pixento.betterhabits.receivers.ReminderAlarmReceiver] reads this synchronously inside
 * `runBlocking`. Querying directly costs the `<queries>` entry in the manifest, without which
 * package-visibility filtering hides the provider on Android 11+.
 *
 * A connection change cannot wake a manifest receiver - `ACTION_CAR_CONNECTION_UPDATED` is an
 * implicit broadcast and not on the API 26+ exception list - so this is read per tick, like
 * [DoNotDisturbMonitor].
 */
class AndroidCarConnectionMonitor(context: Context) : CarConnectionMonitor {

    private val appContext = context.applicationContext

    /**
     * Anything but [NOT_CONNECTED] counts as "in a car", folding projection (Android Auto) and
     * native (Automotive OS) together.
     *
     * Fails open like [AndroidDoNotDisturbMonitor]: a device without Android Auto has no such
     * provider, and neither that nor a refused query may silently kill someone's reminders.
     */
    override fun isConnectedToCar(): Boolean {
        val cursor = try {
            appContext.contentResolver.query(
                CAR_CONNECTION_URI,
                arrayOf(CAR_CONNECTION_STATE_COLUMN),
                null,
                null,
                null,
            )
        } catch (e: Exception) {
            null
        } ?: return false

        return cursor.use {
            if (!it.moveToFirst() || it.columnCount == 0) return@use false
            try {
                it.getInt(0) != NOT_CONNECTED
            } catch (e: Exception) {
                false
            }
        }
    }

    private companion object {
        /** Must stay in step with `CarConnection`'s own authority and column. */
        val CAR_CONNECTION_URI: Uri = Uri.parse("content://androidx.car.app.connection")
        const val CAR_CONNECTION_STATE_COLUMN = "CarConnectionState"

        /** `CONNECTION_TYPE_NOT_CONNECTED`; 1 is native and 2 is projection. */
        const val NOT_CONNECTED = 0
    }
}

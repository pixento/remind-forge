package nl.pixento.betterhabits.alerting

import android.content.Context
import android.net.Uri

/**
 * Reads the car-connection state Android Auto publishes.
 *
 * `UiModeManager.getCurrentModeType()` is deliberately *not* used: Google's own Android Auto FAQ
 * says that on Android 12+ projection no longer changes the device's UI mode, and the
 * `UI_MODE_TYPE_CAR` configuration belongs to the car's virtual display rather than the phone's.
 *
 * The supported API is `androidx.car.app.connection.CarConnection`, which is a thin wrapper over the
 * content provider queried below. The library is not taken as a dependency: it exposes the state
 * only as `LiveData` built from an `@MainThread` constructor, which fights
 * [nl.pixento.betterhabits.receivers.ReminderAlarmReceiver]'s synchronous `runBlocking` tick. A
 * plain query is the same call the library makes, stays synchronous, and matches the shape of
 * [AndroidDoNotDisturbMonitor]. The only thing the library would have contributed is the `<queries>`
 * entry the manifest now declares itself - without which package-visibility filtering hides the
 * provider on Android 11+.
 *
 * Note there is no way to be *told* about a connection change: `ACTION_CAR_CONNECTION_UPDATED` is an
 * implicit broadcast and not on the API 26+ exception list, so a manifest receiver would never fire.
 * Reading it per tick is the only option - the same conclusion this app already reached for Do Not
 * Disturb.
 */
class AndroidCarConnectionMonitor(context: Context) : CarConnectionMonitor {

    private val appContext = context.applicationContext

    /**
     * Anything but [NOT_CONNECTED] counts as "in a car", which folds projection (Android Auto) and
     * native (Automotive OS) together - the same "anything but the normal value counts" reading
     * [AndroidDoNotDisturbMonitor] applies to the interruption filter.
     *
     * Fails open, like [AndroidDoNotDisturbMonitor] and
     * [nl.pixento.betterhabits.scheduling.BatteryOptimization]: a device without Android Auto has no
     * such provider at all, and a missing column or a refused query must not silently kill someone's
     * reminders.
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
        /**
         * Published by the Android Auto app. These are the authority and column
         * `androidx.car.app.connection.CarConnection` itself reads; they're plain strings, not a
         * non-SDK interface, so naming them here costs nothing but has to stay in step with the
         * library's own constants.
         */
        val CAR_CONNECTION_URI: Uri = Uri.parse("content://androidx.car.app.connection")
        const val CAR_CONNECTION_STATE_COLUMN = "CarConnectionState"

        /** `CarConnection.CONNECTION_TYPE_NOT_CONNECTED`; 1 is native and 2 is projection. */
        const val NOT_CONNECTED = 0
    }
}

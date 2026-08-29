package nl.pixento.betterhabits.alerting

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Deep link into the system's own Do Not Disturb settings, so a user following DND can get to the
 * schedule that drives it. Same shape as
 * [nl.pixento.betterhabits.scheduling.ExactAlarmPermission.buildRequestIntent] - build an intent
 * here, start it from the UI.
 *
 * There is no one action that works everywhere, so this walks a fallback chain and returns the
 * first the device can actually handle.
 */
object DoNotDisturbSettings {

    /**
     * The DND screen where the schedules live. `Settings.ACTION_ZEN_MODE_SETTINGS` is @hide, so the
     * constant can't be referenced - but an implicit intent action is just a string, not a non-SDK
     * interface, and this one is handled by AOSP Settings and the common OEM skins. It isn't
     * guaranteed to exist, which is what the resolve check below is for.
     */
    private const val ACTION_ZEN_MODE_SETTINGS = "android.settings.ZEN_MODE_SETTINGS"

    fun buildRequestIntent(context: Context): Intent {
        val candidates = buildList {
            add(Intent(ACTION_ZEN_MODE_SETTINGS))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Public since API 26. Lands inside DND settings, on the behaviour page rather than
                // on the schedules, so it's only the second choice.
                add(Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS))
            }
        }
        // Sound settings has existed since API 1 and is where DND lives in the settings tree, so
        // it's a fallback that always resolves - hence no null return and no hiding of the row.
        return candidates.firstOrNull { it.resolvesOn(context) }
            ?: Intent(Settings.ACTION_SOUND_SETTINGS)
    }

    @Suppress("DEPRECATION") // resolveActivity(Intent, Int); the PackageManager.ResolveInfoFlags
    // overload is API 33+ and this is a plain zero-flags lookup.
    private fun Intent.resolvesOn(context: Context): Boolean =
        context.packageManager.resolveActivity(this, 0) != null
}

package nl.pixento.betterhabits.data.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Translates the removed `active_window_mode` preference into the independent flags that replaced
 * it. The mode used to be an either/or - *custom times* or *follow Do Not Disturb*, never both - so
 * the two arms have to become two separate settings that happen to reproduce the old behaviour.
 *
 * Existing custom-times users deliberately do **not** gain the new Do-Not-Disturb pause: that would
 * change behaviour they configured, on an upgrade they didn't ask for.
 *
 * Runs once: it removes the legacy key, so [shouldMigrate] is false forever after. Doing this as a
 * real one-shot migration rather than a fallback inside [SettingsMapper] matters - a read-side rule
 * would re-apply on every read and overwrite whatever the user picked later.
 */
internal object ActiveWindowModeMigration : DataMigration<Preferences> {

    private val LEGACY_ACTIVE_WINDOW_MODE = stringPreferencesKey("active_window_mode")
    private const val LEGACY_DO_NOT_DISTURB_OFF = "DO_NOT_DISTURB_OFF"

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.contains(LEGACY_ACTIVE_WINDOW_MODE)

    override suspend fun migrate(currentData: Preferences): Preferences {
        val migrated = currentData.toMutablePreferences()
        // The old mode followed Do Not Disturb *instead of* any fixed hours, so it maps to both
        // flags at once; anything else was the plain custom-times mode.
        val followedDoNotDisturb =
            currentData[LEGACY_ACTIVE_WINDOW_MODE] == LEGACY_DO_NOT_DISTURB_OFF
        migrated[PreferencesKeys.LIMIT_TO_ACTIVE_HOURS] = !followedDoNotDisturb
        migrated[PreferencesKeys.PAUSE_DURING_DO_NOT_DISTURB] = followedDoNotDisturb
        migrated.remove(LEGACY_ACTIVE_WINDOW_MODE)
        return migrated.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}

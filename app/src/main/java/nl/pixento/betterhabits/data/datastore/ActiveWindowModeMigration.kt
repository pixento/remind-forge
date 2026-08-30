package nl.pixento.betterhabits.data.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Translates the removed `active_window_mode` preference into the independent flags that replaced
 * it. That mode was an either/or - *custom times* or *follow Do Not Disturb* - so each arm maps to a
 * pair of flags reproducing the behaviour the user had.
 *
 * Custom-times stores deliberately do **not** gain the Do-Not-Disturb pause: that would change
 * behaviour the user configured, on an upgrade they didn't ask for.
 *
 * Runs once: it removes the legacy key, so [shouldMigrate] is false forever after. A read-side
 * fallback in [SettingsMapper] would instead re-apply on every read and overwrite a later choice.
 */
internal object ActiveWindowModeMigration : DataMigration<Preferences> {

    private val LEGACY_ACTIVE_WINDOW_MODE = stringPreferencesKey("active_window_mode")
    private const val LEGACY_DO_NOT_DISTURB_OFF = "DO_NOT_DISTURB_OFF"

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.contains(LEGACY_ACTIVE_WINDOW_MODE)

    override suspend fun migrate(currentData: Preferences): Preferences {
        val migrated = currentData.toMutablePreferences()
        // Following Do Not Disturb excluded fixed hours, so that arm sets both flags at once.
        val followedDoNotDisturb =
            currentData[LEGACY_ACTIVE_WINDOW_MODE] == LEGACY_DO_NOT_DISTURB_OFF
        migrated[PreferencesKeys.LIMIT_TO_ACTIVE_HOURS] = !followedDoNotDisturb
        migrated[PreferencesKeys.PAUSE_DURING_DO_NOT_DISTURB] = followedDoNotDisturb
        migrated.remove(LEGACY_ACTIVE_WINDOW_MODE)
        return migrated.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}

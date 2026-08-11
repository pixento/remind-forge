package nl.pixento.remindforge.data.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import nl.pixento.remindforge.domain.model.VibrationPatternType

/**
 * Translates the removed `alert_mode` preference into the two independent channels that replaced
 * it. Vibration and ringtone used to be mutually exclusive, so a stored ringtone URI meant nothing
 * while the mode was VIBRATION and vice versa - without this, everyone who had picked RINGTONE
 * would start buzzing on upgrade, and stale URIs from an abandoned mode would start ringing.
 *
 * Runs once: it removes the legacy key, so [shouldMigrate] is false forever after. Doing this as a
 * real one-shot migration rather than a fallback inside [SettingsMapper] matters - a read-side rule
 * would re-apply on every read and overwrite whatever the user picked later.
 */
internal object AlertModeMigration : DataMigration<Preferences> {

    private val LEGACY_ALERT_MODE = stringPreferencesKey("alert_mode")
    private const val LEGACY_RINGTONE_MODE = "RINGTONE"

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.contains(LEGACY_ALERT_MODE)

    override suspend fun migrate(currentData: Preferences): Preferences {
        val migrated = currentData.toMutablePreferences()
        if (currentData[LEGACY_ALERT_MODE] == LEGACY_RINGTONE_MODE) {
            migrated[PreferencesKeys.VIBRATION_PATTERN] = VibrationPatternType.SILENT.name
        } else {
            migrated.remove(PreferencesKeys.RINGTONE_URI)
        }
        migrated.remove(LEGACY_ALERT_MODE)
        return migrated.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}

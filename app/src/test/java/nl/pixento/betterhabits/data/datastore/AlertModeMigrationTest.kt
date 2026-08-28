package nl.pixento.betterhabits.data.datastore

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.runTest
import nl.pixento.betterhabits.domain.model.VibrationPatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertModeMigrationTest {

    private val legacyAlertMode = stringPreferencesKey("alert_mode")

    @Test
    fun `migrates only while the legacy key is present`() = runTest {
        assertTrue(
            AlertModeMigration.shouldMigrate(mutablePreferencesOf(legacyAlertMode to "VIBRATION"))
        )
        assertFalse(AlertModeMigration.shouldMigrate(mutablePreferencesOf()))
    }

    @Test
    fun `ringtone mode silences vibration and keeps the ringtone`() = runTest {
        val migrated = AlertModeMigration.migrate(
            mutablePreferencesOf(
                legacyAlertMode to "RINGTONE",
                PreferencesKeys.VIBRATION_PATTERN to VibrationPatternType.LONG_PULSE.name,
                PreferencesKeys.RINGTONE_URI to "content://media/ringtone/1",
            ),
        )

        val settings = SettingsMapper.fromPreferences(migrated)
        assertEquals(VibrationPatternType.SILENT, settings.vibrationPattern)
        assertEquals("content://media/ringtone/1", settings.ringtoneUri)
    }

    @Test
    fun `vibration mode keeps the pattern and drops the stale ringtone`() = runTest {
        val migrated = AlertModeMigration.migrate(
            mutablePreferencesOf(
                legacyAlertMode to "VIBRATION",
                PreferencesKeys.VIBRATION_PATTERN to VibrationPatternType.LONG_PULSE.name,
                PreferencesKeys.RINGTONE_URI to "content://media/ringtone/1",
            ),
        )

        val settings = SettingsMapper.fromPreferences(migrated)
        assertEquals(VibrationPatternType.LONG_PULSE, settings.vibrationPattern)
        assertNull(settings.ringtoneUri)
    }

    @Test
    fun `an unrecognized legacy mode is treated as vibration`() = runTest {
        // Only "RINGTONE" means the ringtone was the live channel. Anything else - including a value
        // this build has never heard of - has to leave the sound channel off rather than start
        // playing a ringtone the user last heard under different settings.
        val migrated = AlertModeMigration.migrate(
            mutablePreferencesOf(
                legacyAlertMode to "SOMETHING_ELSE",
                PreferencesKeys.VIBRATION_PATTERN to VibrationPatternType.LONG_PULSE.name,
                PreferencesKeys.RINGTONE_URI to "content://media/ringtone/1",
            ),
        )

        val settings = SettingsMapper.fromPreferences(migrated)
        assertEquals(VibrationPatternType.LONG_PULSE, settings.vibrationPattern)
        assertNull(settings.ringtoneUri)
    }

    @Test
    fun `legacy key is removed so the migration never runs twice`() = runTest {
        val migrated = AlertModeMigration.migrate(mutablePreferencesOf(legacyAlertMode to "RINGTONE"))

        assertFalse(migrated.contains(legacyAlertMode))
        assertFalse(AlertModeMigration.shouldMigrate(migrated))
    }

    @Test
    fun `other settings are carried across untouched`() = runTest {
        val migrated = AlertModeMigration.migrate(
            mutablePreferencesOf(
                legacyAlertMode to "RINGTONE",
                PreferencesKeys.ENABLED to true,
                PreferencesKeys.INTERVAL_MINUTES to 20,
                PreferencesKeys.WINDOW_START to "08:30",
            ),
        )

        val settings = SettingsMapper.fromPreferences(migrated)
        assertTrue(settings.enabled)
        assertEquals(20, settings.intervalMinutes)
        assertEquals(8, settings.windowStart.hour)
        assertEquals(30, settings.windowStart.minute)
    }
}

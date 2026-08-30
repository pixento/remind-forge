package nl.pixento.betterhabits.data.datastore

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWindowModeMigrationTest {

    private val legacyActiveWindowMode = stringPreferencesKey("active_window_mode")

    @Test
    fun `migrates only while the legacy key is present`() = runTest {
        assertTrue(
            ActiveWindowModeMigration.shouldMigrate(
                mutablePreferencesOf(legacyActiveWindowMode to "CUSTOM_TIMES"),
            )
        )
        assertFalse(ActiveWindowModeMigration.shouldMigrate(mutablePreferencesOf()))
    }

    @Test
    fun `following Do Not Disturb becomes no fixed hours plus the Do Not Disturb pause`() = runTest {
        // The old mode had no time-of-day constraint at all, so the hours have to stay off - and
        // the stored times have to survive, since re-ticking the hours checkbox restores them.
        val migrated = ActiveWindowModeMigration.migrate(
            mutablePreferencesOf(
                legacyActiveWindowMode to "DO_NOT_DISTURB_OFF",
                PreferencesKeys.WINDOW_START to "08:30",
                PreferencesKeys.WINDOW_END to "20:15",
            ),
        )

        val settings = SettingsMapper.fromPreferences(migrated)
        assertFalse(settings.limitToActiveHours)
        assertTrue(settings.pauseDuringDoNotDisturb)
        assertEquals(LocalTime.of(8, 30), settings.windowStart)
        assertEquals(LocalTime.of(20, 15), settings.windowEnd)
    }

    @Test
    fun `custom times keeps its hours and does not gain the Do Not Disturb pause`() = runTest {
        // Firing through Do Not Disturb is what these users configured; an upgrade they didn't ask
        // for must not quietly change that.
        val migrated = ActiveWindowModeMigration.migrate(
            mutablePreferencesOf(legacyActiveWindowMode to "CUSTOM_TIMES"),
        )

        val settings = SettingsMapper.fromPreferences(migrated)
        assertTrue(settings.limitToActiveHours)
        assertFalse(settings.pauseDuringDoNotDisturb)
    }

    @Test
    fun `an unreadable mode falls back to the custom-times arm`() = runTest {
        val migrated = ActiveWindowModeMigration.migrate(
            mutablePreferencesOf(legacyActiveWindowMode to "NOT_A_REAL_MODE"),
        )

        val settings = SettingsMapper.fromPreferences(migrated)
        assertTrue(settings.limitToActiveHours)
        assertFalse(settings.pauseDuringDoNotDisturb)
    }

    @Test
    fun `the legacy key is removed, so the migration runs exactly once`() = runTest {
        val migrated = ActiveWindowModeMigration.migrate(
            mutablePreferencesOf(legacyActiveWindowMode to "DO_NOT_DISTURB_OFF"),
        )

        assertFalse(ActiveWindowModeMigration.shouldMigrate(migrated))
    }

    @Test
    fun `the Android Auto pause is never turned on by the migration`() = runTest {
        // It postdates the mode entirely; nobody upgrading can have asked for it.
        val migrated = ActiveWindowModeMigration.migrate(
            mutablePreferencesOf(legacyActiveWindowMode to "DO_NOT_DISTURB_OFF"),
        )

        assertFalse(SettingsMapper.fromPreferences(migrated).pauseDuringAndroidAuto)
    }
}

package nl.pixento.remindforge.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import nl.pixento.remindforge.domain.model.IntervalRandomness
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl

    private fun setUp() {
        val file = File(tempFolder.root, "test_settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        repository = SettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `defaults are returned before any write`() = runTest {
        setUp()
        assertEquals(ReminderSettings(), repository.settings.first())
    }

    @Test
    fun `each field round-trips through the repository`() = runTest {
        setUp()
        repository.setEnabled(true)
        repository.setIntervalMinutes(20)
        repository.setIntervalRandomness(IntervalRandomness.TWENTY_PERCENT)
        repository.setActiveWindowMode(ActiveWindowMode.DO_NOT_DISTURB_OFF)
        repository.setWindowStart(LocalTime.of(8, 30))
        repository.setWindowEnd(LocalTime.of(20, 15))
        repository.setVibrationPattern(VibrationPatternType.LONG_PULSE)
        repository.setRingtoneUri("content://media/ringtone/1")

        val result = repository.settings.first()
        assertEquals(true, result.enabled)
        assertEquals(20, result.intervalMinutes)
        assertEquals(IntervalRandomness.TWENTY_PERCENT, result.intervalRandomness)
        assertEquals(ActiveWindowMode.DO_NOT_DISTURB_OFF, result.activeWindowMode)
        assertEquals(LocalTime.of(8, 30), result.windowStart)
        assertEquals(LocalTime.of(20, 15), result.windowEnd)
        assertEquals(VibrationPatternType.LONG_PULSE, result.vibrationPattern)
        assertEquals("content://media/ringtone/1", result.ringtoneUri)
    }

    @Test
    fun `setRingtoneUri null clears the stored value`() = runTest {
        setUp()
        repository.setRingtoneUri("content://media/ringtone/1")
        repository.setRingtoneUri(null)
        assertNull(repository.settings.first().ringtoneUri)
    }

    @Test
    fun `interval below minimum is clamped up on write`() = runTest {
        setUp()
        repository.setIntervalMinutes(1)
        assertEquals(
            ReminderSettings.MIN_INTERVAL_MINUTES,
            repository.settings.first().intervalMinutes
        )
    }

    @Test
    fun `interval above maximum is clamped down on write`() = runTest {
        setUp()
        repository.setIntervalMinutes(999)
        assertEquals(
            ReminderSettings.MAX_INTERVAL_MINUTES,
            repository.settings.first().intervalMinutes
        )
    }

    @Test
    fun `corrupted vibration pattern string falls back to default`() = runTest {
        setUp()
        dataStore.edit { it[stringPreferencesKey("vibration_pattern")] = "NOT_A_REAL_PATTERN" }
        assertEquals(
            ReminderSettings().vibrationPattern,
            repository.settings.first().vibrationPattern
        )
    }

    @Test
    fun `a store without the active window mode key reads as custom times`() = runTest {
        // The key is purely additive, which is why it needs no DataMigration: every store written
        // before it existed has to keep behaving exactly as it did.
        setUp()
        dataStore.edit { it[stringPreferencesKey("window_start")] = "08:00" }
        assertEquals(ActiveWindowMode.CUSTOM_TIMES, repository.settings.first().activeWindowMode)
    }

    @Test
    fun `a store without the interval randomness key reads as none`() = runTest {
        // Additive too, so no DataMigration: a store written before randomness existed has to keep
        // firing on the exact cadence it always did.
        setUp()
        dataStore.edit { it[intPreferencesKey("interval_minutes")] = 20 }
        assertEquals(IntervalRandomness.NONE, repository.settings.first().intervalRandomness)
    }

    @Test
    fun `corrupted interval randomness string falls back to none`() = runTest {
        setUp()
        dataStore.edit { it[stringPreferencesKey("interval_randomness")] = "NINETY_PERCENT" }
        assertEquals(IntervalRandomness.NONE, repository.settings.first().intervalRandomness)
    }

    @Test
    fun `corrupted active window mode string falls back to default`() = runTest {
        setUp()
        dataStore.edit { it[stringPreferencesKey("active_window_mode")] = "NOT_A_REAL_MODE" }
        assertEquals(ActiveWindowMode.CUSTOM_TIMES, repository.settings.first().activeWindowMode)
    }

    @Test
    fun `corrupted window time string falls back to default`() = runTest {
        setUp()
        dataStore.edit { it[stringPreferencesKey("window_start")] = "not-a-time" }
        assertEquals(ReminderSettings().windowStart, repository.settings.first().windowStart)
    }

    @Test
    fun `corrupted window end string falls back to default too`() = runTest {
        setUp()
        dataStore.edit { it[stringPreferencesKey("window_end")] = "not-a-time" }
        assertEquals(ReminderSettings().windowEnd, repository.settings.first().windowEnd)
    }

    @Test
    fun `a single-digit hour is not a valid stored time`() = runTest {
        // Times are stored strictly as HH:mm, so "9:00" is corruption rather than a lenient 09:00 -
        // silently accepting it would let two encodings of the same time into the store.
        setUp()
        dataStore.edit { it[stringPreferencesKey("window_start")] = "9:00" }
        assertEquals(ReminderSettings().windowStart, repository.settings.first().windowStart)
    }

    @Test
    fun `an out-of-range stored interval is clamped on read`() = runTest {
        // The setter clamps too, but a value written by an older build (or straight into the file)
        // still has to come back inside the range the calculator will accept.
        setUp()
        dataStore.edit { it[intPreferencesKey("interval_minutes")] = 999 }
        assertEquals(
            ReminderSettings.MAX_INTERVAL_MINUTES,
            repository.settings.first().intervalMinutes,
        )

        dataStore.edit { it[intPreferencesKey("interval_minutes")] = 0 }
        assertEquals(
            ReminderSettings.MIN_INTERVAL_MINUTES,
            repository.settings.first().intervalMinutes,
        )
    }
}

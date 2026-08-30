package nl.pixento.betterhabits.data.datastore

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
import nl.pixento.betterhabits.domain.model.IntervalRandomness
import nl.pixento.betterhabits.domain.model.ReminderSettings
import nl.pixento.betterhabits.domain.model.VibrationPatternType
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
        repository.setLimitToActiveHours(false)
        repository.setWindowStart(LocalTime.of(8, 30))
        repository.setWindowEnd(LocalTime.of(20, 15))
        repository.setPauseDuringDoNotDisturb(true)
        repository.setPauseDuringAndroidAuto(true)
        repository.setVibrationPattern(VibrationPatternType.LONG_PULSE)
        repository.setRingtoneUri("content://media/ringtone/1")

        val result = repository.settings.first()
        assertEquals(true, result.enabled)
        assertEquals(20, result.intervalMinutes)
        assertEquals(IntervalRandomness.TWENTY_PERCENT, result.intervalRandomness)
        assertEquals(false, result.limitToActiveHours)
        assertEquals(LocalTime.of(8, 30), result.windowStart)
        assertEquals(LocalTime.of(20, 15), result.windowEnd)
        assertEquals(true, result.pauseDuringDoNotDisturb)
        assertEquals(true, result.pauseDuringAndroidAuto)
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
    fun `a store without the pause keys reads as hours-only`() = runTest {
        // The defaults have to reproduce what a store written before these keys existed did:
        // fixed hours, and nothing pausing them. ActiveWindowModeMigration handles the one store
        // shape where that isn't true.
        setUp()
        dataStore.edit { it[stringPreferencesKey("window_start")] = "08:00" }
        val result = repository.settings.first()
        assertEquals(true, result.limitToActiveHours)
        assertEquals(false, result.pauseDuringDoNotDisturb)
        assertEquals(false, result.pauseDuringAndroidAuto)
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

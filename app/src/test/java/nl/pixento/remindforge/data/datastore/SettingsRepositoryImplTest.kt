package nl.pixento.remindforge.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
        repository.setWindowStart(LocalTime.of(8, 30))
        repository.setWindowEnd(LocalTime.of(20, 15))
        repository.setVibrationPattern(VibrationPatternType.LONG_PULSE)
        repository.setRingtoneUri("content://media/ringtone/1")

        val result = repository.settings.first()
        assertEquals(true, result.enabled)
        assertEquals(20, result.intervalMinutes)
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
    fun `corrupted window time string falls back to default`() = runTest {
        setUp()
        dataStore.edit { it[stringPreferencesKey("window_start")] = "not-a-time" }
        assertEquals(ReminderSettings().windowStart, repository.settings.first().windowStart)
    }
}

package nl.pixento.betterhabits.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.pixento.betterhabits.data.SettingsRepository
import nl.pixento.betterhabits.domain.model.IntervalRandomness
import nl.pixento.betterhabits.domain.model.ReminderSettings
import nl.pixento.betterhabits.domain.model.VibrationPatternType

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<ReminderSettings> =
        dataStore.data.map { SettingsMapper.fromPreferences(it) }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ENABLED] = enabled }
    }

    override suspend fun setIntervalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(
            ReminderSettings.MIN_INTERVAL_MINUTES,
            ReminderSettings.MAX_INTERVAL_MINUTES,
        )
        dataStore.edit { it[PreferencesKeys.INTERVAL_MINUTES] = clamped }
    }

    override suspend fun setIntervalRandomness(randomness: IntervalRandomness) {
        dataStore.edit { it[PreferencesKeys.INTERVAL_RANDOMNESS] = randomness.name }
    }

    override suspend fun setLimitToActiveHours(limit: Boolean) {
        dataStore.edit { it[PreferencesKeys.LIMIT_TO_ACTIVE_HOURS] = limit }
    }

    override suspend fun setWindowStart(time: LocalTime) {
        dataStore.edit { it[PreferencesKeys.WINDOW_START] = SettingsMapper.formatTime(time) }
    }

    override suspend fun setWindowEnd(time: LocalTime) {
        dataStore.edit { it[PreferencesKeys.WINDOW_END] = SettingsMapper.formatTime(time) }
    }

    override suspend fun setPauseDuringDoNotDisturb(pause: Boolean) {
        dataStore.edit { it[PreferencesKeys.PAUSE_DURING_DO_NOT_DISTURB] = pause }
    }

    override suspend fun setPauseDuringAndroidAuto(pause: Boolean) {
        dataStore.edit { it[PreferencesKeys.PAUSE_DURING_ANDROID_AUTO] = pause }
    }

    override suspend fun setVibrationPattern(pattern: VibrationPatternType) {
        dataStore.edit { it[PreferencesKeys.VIBRATION_PATTERN] = pattern.name }
    }

    override suspend fun setRingtoneUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(PreferencesKeys.RINGTONE_URI)
            } else {
                prefs[PreferencesKeys.RINGTONE_URI] = uri
            }
        }
    }
}

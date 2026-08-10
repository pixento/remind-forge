package nl.pixento.remindforge.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.AlertMode
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType

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

    override suspend fun setWindowStart(time: LocalTime) {
        dataStore.edit { it[PreferencesKeys.WINDOW_START] = SettingsMapper.formatTime(time) }
    }

    override suspend fun setWindowEnd(time: LocalTime) {
        dataStore.edit { it[PreferencesKeys.WINDOW_END] = SettingsMapper.formatTime(time) }
    }

    override suspend fun setAlertMode(mode: AlertMode) {
        dataStore.edit { it[PreferencesKeys.ALERT_MODE] = mode.name }
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

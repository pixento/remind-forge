package nl.pixento.remindforge.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object PreferencesKeys {
    val ENABLED = booleanPreferencesKey("enabled")
    val INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
    val WINDOW_START = stringPreferencesKey("window_start")
    val WINDOW_END = stringPreferencesKey("window_end")
    val VIBRATION_PATTERN = stringPreferencesKey("vibration_pattern")
    val RINGTONE_URI = stringPreferencesKey("ringtone_uri")

    /** Alarm-chain state rather than a user setting; see ScheduleStateRepository. */
    val NEXT_TRIGGER_AT_MILLIS = longPreferencesKey("next_trigger_at_millis")
}

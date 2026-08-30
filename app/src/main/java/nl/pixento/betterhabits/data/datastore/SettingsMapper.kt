package nl.pixento.betterhabits.data.datastore

import androidx.datastore.preferences.core.Preferences
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import nl.pixento.betterhabits.domain.model.ReminderSettings

internal object SettingsMapper {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun fromPreferences(preferences: Preferences): ReminderSettings {
        val defaults = ReminderSettings()
        return ReminderSettings(
            enabled = preferences[PreferencesKeys.ENABLED] ?: defaults.enabled,
            intervalMinutes = (preferences[PreferencesKeys.INTERVAL_MINUTES]
                ?: defaults.intervalMinutes)
                .coerceIn(
                    ReminderSettings.MIN_INTERVAL_MINUTES,
                    ReminderSettings.MAX_INTERVAL_MINUTES
                ),
            intervalRandomness = parseEnumOrDefault(
                preferences[PreferencesKeys.INTERVAL_RANDOMNESS],
                defaults.intervalRandomness,
            ),
            limitToActiveHours = preferences[PreferencesKeys.LIMIT_TO_ACTIVE_HOURS]
                ?: defaults.limitToActiveHours,
            windowStart = parseTimeOrDefault(
                preferences[PreferencesKeys.WINDOW_START],
                defaults.windowStart
            ),
            windowEnd = parseTimeOrDefault(
                preferences[PreferencesKeys.WINDOW_END],
                defaults.windowEnd
            ),
            pauseDuringDoNotDisturb = preferences[PreferencesKeys.PAUSE_DURING_DO_NOT_DISTURB]
                ?: defaults.pauseDuringDoNotDisturb,
            pauseDuringAndroidAuto = preferences[PreferencesKeys.PAUSE_DURING_ANDROID_AUTO]
                ?: defaults.pauseDuringAndroidAuto,
            vibrationPattern = parseEnumOrDefault(
                preferences[PreferencesKeys.VIBRATION_PATTERN],
                defaults.vibrationPattern,
            ),
            ringtoneUri = preferences[PreferencesKeys.RINGTONE_URI],
        )
    }

    fun formatTime(time: LocalTime): String = time.format(timeFormatter)

    private fun parseTimeOrDefault(raw: String?, default: LocalTime): LocalTime {
        if (raw == null) return default
        return try {
            LocalTime.parse(raw, timeFormatter)
        } catch (e: DateTimeParseException) {
            default
        }
    }

    private inline fun <reified T : Enum<T>> parseEnumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return try {
            enumValueOf<T>(raw)
        } catch (e: IllegalArgumentException) {
            default
        }
    }
}

package nl.pixento.remindforge.data

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import nl.pixento.remindforge.domain.model.AlertMode
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType

interface SettingsRepository {
    val settings: Flow<ReminderSettings>

    suspend fun setEnabled(enabled: Boolean)
    suspend fun setIntervalMinutes(minutes: Int)
    suspend fun setWindowStart(time: LocalTime)
    suspend fun setWindowEnd(time: LocalTime)
    suspend fun setAlertMode(mode: AlertMode)
    suspend fun setVibrationPattern(pattern: VibrationPatternType)
    suspend fun setRingtoneUri(uri: String?)
}

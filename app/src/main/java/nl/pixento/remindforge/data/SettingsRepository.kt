package nl.pixento.remindforge.data

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import nl.pixento.remindforge.domain.model.IntervalRandomness
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType

interface SettingsRepository {
    val settings: Flow<ReminderSettings>

    suspend fun setEnabled(enabled: Boolean)
    suspend fun setIntervalMinutes(minutes: Int)
    suspend fun setIntervalRandomness(randomness: IntervalRandomness)
    suspend fun setActiveWindowMode(mode: ActiveWindowMode)
    suspend fun setWindowStart(time: LocalTime)
    suspend fun setWindowEnd(time: LocalTime)
    suspend fun setVibrationPattern(pattern: VibrationPatternType)
    suspend fun setRingtoneUri(uri: String?)
}

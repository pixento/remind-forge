package nl.pixento.betterhabits.data

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import nl.pixento.betterhabits.domain.model.ActiveWindowMode
import nl.pixento.betterhabits.domain.model.IntervalRandomness
import nl.pixento.betterhabits.domain.model.ReminderSettings
import nl.pixento.betterhabits.domain.model.VibrationPatternType

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

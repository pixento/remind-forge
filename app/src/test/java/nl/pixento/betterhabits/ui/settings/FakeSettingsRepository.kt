package nl.pixento.betterhabits.ui.settings

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import nl.pixento.betterhabits.data.SettingsRepository
import nl.pixento.betterhabits.domain.model.ActiveWindowMode
import nl.pixento.betterhabits.domain.model.IntervalRandomness
import nl.pixento.betterhabits.domain.model.ReminderSettings
import nl.pixento.betterhabits.domain.model.VibrationPatternType

/** In-memory test double mirroring [SettingsRepositoryImpl]'s clamping behavior. */
class FakeSettingsRepository(
    initial: ReminderSettings = ReminderSettings(),
) : SettingsRepository {

    private val state = MutableStateFlow(initial)
    override val settings: Flow<ReminderSettings> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled)
    }

    override suspend fun setIntervalMinutes(minutes: Int) {
        state.value = state.value.copy(
            intervalMinutes = minutes.coerceIn(
                ReminderSettings.MIN_INTERVAL_MINUTES,
                ReminderSettings.MAX_INTERVAL_MINUTES,
            ),
        )
    }

    override suspend fun setIntervalRandomness(randomness: IntervalRandomness) {
        state.value = state.value.copy(intervalRandomness = randomness)
    }

    override suspend fun setActiveWindowMode(mode: ActiveWindowMode) {
        state.value = state.value.copy(activeWindowMode = mode)
    }

    override suspend fun setWindowStart(time: LocalTime) {
        state.value = state.value.copy(windowStart = time)
    }

    override suspend fun setWindowEnd(time: LocalTime) {
        state.value = state.value.copy(windowEnd = time)
    }

    override suspend fun setVibrationPattern(pattern: VibrationPatternType) {
        state.value = state.value.copy(vibrationPattern = pattern)
    }

    override suspend fun setRingtoneUri(uri: String?) {
        state.value = state.value.copy(ringtoneUri = uri)
    }
}

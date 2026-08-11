package nl.pixento.remindforge.ui.settings

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType

/** In-memory test double mirroring [SettingsRepositoryImpl]'s clamping behavior. */
class FakeSettingsRepository(
    initial: ReminderSettings = ReminderSettings(),
) : SettingsRepository {

    private val state = MutableStateFlow(initial)
    override val settings: Flow<ReminderSettings> = state

    val current: ReminderSettings get() = state.value

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

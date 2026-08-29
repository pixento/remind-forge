package nl.pixento.betterhabits.ui.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import nl.pixento.betterhabits.data.ScheduleStateRepository

/** In-memory test double for [ScheduleStateRepository]. */
class FakeScheduleStateRepository(
    initial: Long? = null,
) : ScheduleStateRepository {

    private val state = MutableStateFlow(initial)
    override val nextTriggerAtMillis: Flow<Long?> = state

    override suspend fun setNextTriggerAtMillis(millis: Long?) {
        state.value = millis
    }
}

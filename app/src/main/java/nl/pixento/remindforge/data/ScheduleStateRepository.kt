package nl.pixento.remindforge.data

import kotlinx.coroutines.flow.Flow

/**
 * The alarm chain's own state, as opposed to the user's [SettingsRepository] settings: the instant
 * the currently pending alarm will fire. `AlarmManager` has no cross-process query for a pending
 * alarm's trigger time, so whoever schedules a tick records it here - otherwise the Settings screen
 * can only guess at "next reminder", and its guess (now + interval) drifts away from the real chain
 * every time the screen is opened.
 */
interface ScheduleStateRepository {
    /** Epoch millis of the pending alarm, or `null` when no alarm is scheduled. */
    val nextTriggerAtMillis: Flow<Long?>

    suspend fun setNextTriggerAtMillis(millis: Long?)
}

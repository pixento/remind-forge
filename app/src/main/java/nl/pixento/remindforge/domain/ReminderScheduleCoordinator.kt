package nl.pixento.remindforge.domain

import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import nl.pixento.remindforge.data.ScheduleStateRepository
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.scheduling.AlarmScheduler

/**
 * Single entry point for (re)starting the alarm chain: reads current settings and either
 * cancels (if disabled) or cancels + reschedules from *now* (if enabled). Used on enable, on
 * disable, on a change to one of the settings the chain is computed from (interval and window),
 * and on boot/app-update - so such a change takes effect immediately rather than waiting for the
 * old chain to finish its current interval.
 *
 * Deliberately *not* used for settings the chain doesn't depend on (vibration pattern, ringtone):
 * restarting from now would push the next reminder a full interval into the future, so those are
 * simply picked up by the next tick, which re-reads settings anyway.
 */
class ReminderScheduleCoordinator(
    private val settingsRepository: SettingsRepository,
    private val scheduleStateRepository: ScheduleStateRepository,
    private val alarmScheduler: AlarmScheduler,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = Instant::now,
) {

    suspend fun rescheduleFromNow() {
        val settings = settingsRepository.settings.first()
        if (!settings.enabled) {
            alarmScheduler.cancel()
            scheduleStateRepository.setNextTriggerAtMillis(null)
            return
        }

        val next = NextTriggerCalculator.nextTrigger(
            referenceInstant = now(),
            zone = zone,
            intervalMinutes = settings.intervalMinutes,
            windowStart = settings.windowStart,
            windowEnd = settings.windowEnd,
        )
        alarmScheduler.scheduleNext(next.toEpochMilli())
        scheduleStateRepository.setNextTriggerAtMillis(next.toEpochMilli())
    }

    /**
     * Repairs a chain that died silently (revoked permission, process death mid-tick, OEM
     * background kill) without disturbing one that's still intact. Safe to call on every app
     * start: a no-op unless enabled and the chain is actually broken.
     *
     * A pending alarm with no recorded trigger time counts as broken even though the alarm itself
     * would still fire: `AlarmManager` won't tell us when that is, so the chain's state can only be
     * made consistent by restarting it. In practice this happens once, on upgrade from a version
     * that didn't record the trigger time.
     */
    suspend fun healIfNeeded() {
        val settings = settingsRepository.settings.first()
        if (!settings.enabled) return

        val intact = alarmScheduler.hasPending() &&
                scheduleStateRepository.nextTriggerAtMillis.first() != null
        if (!intact) rescheduleFromNow()
    }
}

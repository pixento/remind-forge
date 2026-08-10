package nl.pixento.remindforge.domain

import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.scheduling.AlarmScheduler

/**
 * Single entry point for (re)starting the alarm chain: reads current settings and either
 * cancels (if disabled) or cancels + reschedules from *now* (if enabled). Used identically on
 * enable, on any settings change while enabled, on disable, and on boot/app-update - so a
 * settings change always takes effect immediately rather than waiting for the old chain to
 * finish its current interval.
 */
class ReminderScheduleCoordinator(
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = Instant::now,
) {

    suspend fun rescheduleFromNow() {
        val settings = settingsRepository.settings.first()
        if (!settings.enabled) {
            alarmScheduler.cancel()
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
    }
}

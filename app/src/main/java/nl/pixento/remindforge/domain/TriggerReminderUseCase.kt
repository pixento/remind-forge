package nl.pixento.remindforge.domain

import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import nl.pixento.remindforge.alerting.AlertPlayer
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.AlertMode
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.scheduling.AlarmScheduler

/** Why a given alarm-chain tick did or didn't play an alert, for the receiver to log. */
enum class AlarmFiredOutcome { FIRED, DISABLED, OUTSIDE_WINDOW, NO_RINGTONE_URI }

/**
 * Handles a single alarm-chain tick: re-checks current settings (they may have changed since
 * this alarm was scheduled), plays the alert if still enabled and within the active window,
 * and always computes + schedules the next tick from the *scheduled* time so drift doesn't
 * compound across the chain.
 */
class TriggerReminderUseCase(
    private val settingsRepository: SettingsRepository,
    private val alertPlayer: AlertPlayer,
    private val alarmScheduler: AlarmScheduler,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = Instant::now,
) {

    suspend fun onAlarmFired(scheduledAtMillis: Long): AlarmFiredOutcome {
        val settings = settingsRepository.settings.first()
        if (!settings.enabled) return AlarmFiredOutcome.DISABLED

        val outcome = if (
            NextTriggerCalculator.isWithinWindow(
                now(),
                zone,
                settings.windowStart,
                settings.windowEnd
            )
        ) {
            playAlert(settings)
        } else {
            AlarmFiredOutcome.OUTSIDE_WINDOW
        }

        val next = NextTriggerCalculator.nextTrigger(
            referenceInstant = Instant.ofEpochMilli(scheduledAtMillis),
            zone = zone,
            intervalMinutes = settings.intervalMinutes,
            windowStart = settings.windowStart,
            windowEnd = settings.windowEnd,
        )
        alarmScheduler.scheduleNext(next.toEpochMilli())
        return outcome
    }

    private fun playAlert(settings: ReminderSettings): AlarmFiredOutcome {
        when (settings.alertMode) {
            AlertMode.VIBRATION -> alertPlayer.playVibration(settings.vibrationPattern)
            AlertMode.RINGTONE -> {
                val uri = settings.ringtoneUri ?: return AlarmFiredOutcome.NO_RINGTONE_URI
                alertPlayer.playRingtone(uri)
            }
        }
        return AlarmFiredOutcome.FIRED
    }
}

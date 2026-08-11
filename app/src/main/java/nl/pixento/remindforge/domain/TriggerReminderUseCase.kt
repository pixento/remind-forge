package nl.pixento.remindforge.domain

import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import nl.pixento.remindforge.alerting.AlertPlayer
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.AlarmScheduler

/** Why a given alarm-chain tick did or didn't play an alert, for the receiver to log. */
enum class AlarmFiredOutcome { FIRED, DISABLED, OUTSIDE_WINDOW, NO_ALERT_SELECTED }

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

        // One reading of the clock for both the window check and the catch-up below, so a tick
        // can't be judged in-window and then rescheduled against a slightly different "now".
        val firedAt = now()

        val outcome = if (
            NextTriggerCalculator.isWithinWindow(
                firedAt,
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
            now = firedAt,
        )
        alarmScheduler.scheduleNext(next.toEpochMilli())
        return outcome
    }

    /**
     * Vibration and sound are independent channels, so a tick can buzz, ring, or do both. Silencing
     * both is a legitimate (if odd) configuration the settings screen warns about, not an error -
     * the chain still reschedules either way.
     */
    private fun playAlert(settings: ReminderSettings): AlarmFiredOutcome {
        var played = false
        if (settings.vibrationPattern != VibrationPatternType.SILENT) {
            alertPlayer.playVibration(settings.vibrationPattern)
            played = true
        }
        settings.ringtoneUri?.let { uri ->
            alertPlayer.playRingtone(uri)
            played = true
        }
        return if (played) AlarmFiredOutcome.FIRED else AlarmFiredOutcome.NO_ALERT_SELECTED
    }
}

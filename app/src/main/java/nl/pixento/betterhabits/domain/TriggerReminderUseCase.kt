package nl.pixento.betterhabits.domain

import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import nl.pixento.betterhabits.alerting.AlertPlayer
import nl.pixento.betterhabits.alerting.CarConnectionMonitor
import nl.pixento.betterhabits.alerting.DoNotDisturbMonitor
import nl.pixento.betterhabits.data.ScheduleStateRepository
import nl.pixento.betterhabits.data.SettingsRepository
import nl.pixento.betterhabits.domain.model.ReminderSettings
import nl.pixento.betterhabits.domain.model.VibrationPatternType
import nl.pixento.betterhabits.scheduling.AlarmScheduler

/** Why a given alarm-chain tick did or didn't play an alert, for the receiver to log. */
enum class AlarmFiredOutcome {
    FIRED,
    DISABLED,
    OUTSIDE_WINDOW,
    DO_NOT_DISTURB,
    ANDROID_AUTO,
    NO_ALERT_SELECTED,
}

/**
 * Handles a single alarm-chain tick: re-checks current settings (they may have changed since
 * this alarm was scheduled), plays the alert if still enabled and none of the pause conditions
 * apply, and always computes + schedules the next tick from the *scheduled* time so drift doesn't
 * compound across the chain.
 *
 * The three conditions - active hours, Do Not Disturb, a connected car - are independent and each
 * separately opted into, so a user can ask for "09:00-17:00, *and* quiet whenever Do Not Disturb is
 * on". Only the hours can be predicted for a future instant; the other two are readable only for
 * *now*, which is why they are judged here per tick rather than baked into the next trigger time.
 */
class TriggerReminderUseCase(
    private val settingsRepository: SettingsRepository,
    private val scheduleStateRepository: ScheduleStateRepository,
    private val alertPlayer: AlertPlayer,
    private val alarmScheduler: AlarmScheduler,
    private val doNotDisturbMonitor: DoNotDisturbMonitor,
    private val carConnectionMonitor: CarConnectionMonitor,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = Instant::now,
    private val random: Random = Random.Default,
) {

    suspend fun onAlarmFired(scheduledAtMillis: Long): AlarmFiredOutcome {
        val settings = settingsRepository.settings.first()
        if (!settings.enabled) {
            // The chain ends here, so nothing is pending any more.
            scheduleStateRepository.setNextTriggerAtMillis(null)
            return AlarmFiredOutcome.DISABLED
        }

        // One reading of the clock for both the window check and the catch-up below, so a tick
        // can't be judged in-window and then rescheduled against a slightly different "now".
        val firedAt = now()

        // Cheapest first: the window check is pure math, so a tick outside the active hours costs
        // no system calls at all. Each of the other two is asked of the OS only when the user opted
        // into it, so nothing is read on behalf of someone who didn't ask for that condition.
        val outcome = when {
            !NextTriggerCalculator.isWithinWindow(firedAt, zone, settings.activeWindow) ->
                AlarmFiredOutcome.OUTSIDE_WINDOW

            settings.pauseDuringDoNotDisturb && doNotDisturbMonitor.isDoNotDisturbActive() ->
                AlarmFiredOutcome.DO_NOT_DISTURB

            settings.pauseDuringAndroidAuto && carConnectionMonitor.isConnectedToCar() ->
                AlarmFiredOutcome.ANDROID_AUTO

            else -> playAlert(settings)
        }

        val next = NextTriggerCalculator.nextTrigger(
            referenceInstant = Instant.ofEpochMilli(scheduledAtMillis),
            zone = zone,
            intervalMinutes = settings.intervalMinutes,
            window = settings.activeWindow,
            now = firedAt,
            randomness = settings.intervalRandomness,
            random = random,
        )
        alarmScheduler.scheduleNext(next.toEpochMilli())
        scheduleStateRepository.setNextTriggerAtMillis(next.toEpochMilli())
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

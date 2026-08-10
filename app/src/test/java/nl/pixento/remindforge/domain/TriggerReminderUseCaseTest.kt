package nl.pixento.remindforge.domain

import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.pixento.remindforge.alerting.AlertPlayer
import nl.pixento.remindforge.alerting.ReminderNotifier
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.AlertMode
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.AlarmScheduler
import org.junit.Test

class TriggerReminderUseCaseTest {

    private val zone = ZoneOffset.UTC
    private val settingsRepository = mockk<SettingsRepository>()
    private val alertPlayer = mockk<AlertPlayer>(relaxUnitFun = true)
    private val reminderNotifier = mockk<ReminderNotifier>(relaxUnitFun = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)

    private fun useCase(fixedNow: Instant) = TriggerReminderUseCase(
        settingsRepository = settingsRepository,
        alertPlayer = alertPlayer,
        reminderNotifier = reminderNotifier,
        alarmScheduler = alarmScheduler,
        zone = zone,
        now = { fixedNow },
    )

    private fun scheduledInstant(hour: Int, minute: Int) =
        Instant.parse("2026-01-01T${"%02d".format(hour)}:${"%02d".format(minute)}:00Z")

    @Test
    fun `enabled and vibration mode plays vibration and notifies`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            alertMode = AlertMode.VIBRATION,
            vibrationPattern = VibrationPatternType.LONG_PULSE,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        verify { alertPlayer.playVibration(VibrationPatternType.LONG_PULSE) }
        verify { reminderNotifier.showNotification() }
        verify(exactly = 0) { alertPlayer.playRingtone(any()) }
    }

    @Test
    fun `enabled and ringtone mode plays ringtone and notifies`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            alertMode = AlertMode.RINGTONE,
            ringtoneUri = "content://media/ringtone/1",
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        verify { alertPlayer.playRingtone("content://media/ringtone/1") }
        verify { reminderNotifier.showNotification() }
        verify(exactly = 0) { alertPlayer.playVibration(any()) }
    }

    @Test
    fun `disabled settings skip alert and do not reschedule`() = runTest {
        val settings = ReminderSettings(enabled = false)
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        verify(exactly = 0) { alertPlayer.playVibration(any()) }
        verify(exactly = 0) { alertPlayer.playRingtone(any()) }
        verify(exactly = 0) { reminderNotifier.showNotification() }
        verify(exactly = 0) { alarmScheduler.scheduleNext(any()) }
    }

    @Test
    fun `window mismatch at fire time skips alert but still reschedules`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)
        // "now" has drifted outside the window even though the alarm was scheduled inside it.
        val driftedNow = scheduledInstant(20, 0)

        useCase(fixedNow = driftedNow).onAlarmFired(scheduledAt.toEpochMilli())

        verify(exactly = 0) { alertPlayer.playVibration(any()) }
        verify(exactly = 0) { reminderNotifier.showNotification() }
        coVerify { alarmScheduler.scheduleNext(any()) }
    }

    @Test
    fun `reschedule uses the scheduled-time reference, not wall clock`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)
        val actualFireTime = scheduledAt.plusSeconds(40) // some jitter before it actually ran

        useCase(fixedNow = actualFireTime).onAlarmFired(scheduledAt.toEpochMilli())

        val expectedNext = scheduledAt.plusSeconds(15 * 60).toEpochMilli()
        verify { alarmScheduler.scheduleNext(expectedNext) }
    }
}

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
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerReminderUseCaseTest {

    private val zone = ZoneOffset.UTC
    private val settingsRepository = mockk<SettingsRepository>()
    private val alertPlayer = mockk<AlertPlayer>(relaxUnitFun = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)

    private fun useCase(fixedNow: Instant) = TriggerReminderUseCase(
        settingsRepository = settingsRepository,
        alertPlayer = alertPlayer,
        alarmScheduler = alarmScheduler,
        zone = zone,
        now = { fixedNow },
    )

    private fun scheduledInstant(hour: Int, minute: Int) =
        Instant.parse("2026-01-01T${"%02d".format(hour)}:${"%02d".format(minute)}:00Z")

    @Test
    fun `a pattern and a ringtone both play on the same tick`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
            ringtoneUri = "content://media/ringtone/1",
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.FIRED, outcome)
        verify { alertPlayer.playVibration(VibrationPatternType.LONG_PULSE) }
        verify { alertPlayer.playRingtone("content://media/ringtone/1") }
    }

    @Test
    fun `a pattern with no ringtone only vibrates`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
            ringtoneUri = null,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.FIRED, outcome)
        verify { alertPlayer.playVibration(VibrationPatternType.LONG_PULSE) }
        verify(exactly = 0) { alertPlayer.playRingtone(any()) }
    }

    @Test
    fun `a silent pattern with a ringtone only rings`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.SILENT,
            ringtoneUri = "content://media/ringtone/1",
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.FIRED, outcome)
        verify { alertPlayer.playRingtone("content://media/ringtone/1") }
        verify(exactly = 0) { alertPlayer.playVibration(any()) }
    }

    @Test
    fun `both channels silent skips the alert but still reschedules`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.SILENT,
            ringtoneUri = null,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.NO_ALERT_SELECTED, outcome)
        verify(exactly = 0) { alertPlayer.playRingtone(any()) }
        verify(exactly = 0) { alertPlayer.playVibration(any()) }
        coVerify { alarmScheduler.scheduleNext(any()) }
    }

    @Test
    fun `disabled settings skip alert and do not reschedule`() = runTest {
        val settings = ReminderSettings(enabled = false)
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.DISABLED, outcome)
        verify(exactly = 0) { alertPlayer.playVibration(any()) }
        verify(exactly = 0) { alertPlayer.playRingtone(any()) }
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

        val outcome = useCase(fixedNow = driftedNow).onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.OUTSIDE_WINDOW, outcome)
        verify(exactly = 0) { alertPlayer.playVibration(any()) }
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

    @Test
    fun `a tick delivered late reschedules into the future, not the past`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)
        // Doze held the alarm well past the 10:15 slot; scheduling it would fire instantly and
        // the chain would replay every missed tick back to back.
        val lateFireTime = scheduledInstant(10, 32)

        useCase(fixedNow = lateFireTime).onAlarmFired(scheduledAt.toEpochMilli())

        verify { alarmScheduler.scheduleNext(scheduledInstant(10, 45).toEpochMilli()) }
    }
}

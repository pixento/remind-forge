package nl.pixento.remindforge.domain

import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.pixento.remindforge.alerting.AlertPlayer
import nl.pixento.remindforge.alerting.DoNotDisturbMonitor
import nl.pixento.remindforge.data.ScheduleStateRepository
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import nl.pixento.remindforge.domain.model.IntervalRandomness
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerReminderUseCaseTest {

    private val zone = ZoneOffset.UTC
    private val settingsRepository = mockk<SettingsRepository>()
    private val scheduleStateRepository = mockk<ScheduleStateRepository>(relaxUnitFun = true)
    private val alertPlayer = mockk<AlertPlayer>(relaxUnitFun = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)

    private fun useCase(
        fixedNow: Instant,
        doNotDisturbActive: Boolean = false,
        random: Random = Random.Default,
    ) =
        TriggerReminderUseCase(
            settingsRepository = settingsRepository,
            scheduleStateRepository = scheduleStateRepository,
            alertPlayer = alertPlayer,
            alarmScheduler = alarmScheduler,
            doNotDisturbMonitor = FakeDoNotDisturbMonitor(doNotDisturbActive),
            zone = zone,
            now = { fixedNow },
            random = random,
        )

    private class FakeDoNotDisturbMonitor(private val active: Boolean) : DoNotDisturbMonitor {
        override fun isDoNotDisturbActive(): Boolean = active
    }

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
    fun `following Do Not Disturb, an active filter skips the alert but still reschedules`() =
        runTest {
            val settings = ReminderSettings(
                enabled = true,
                intervalMinutes = 15,
                activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF,
                vibrationPattern = VibrationPatternType.LONG_PULSE,
                ringtoneUri = "content://media/ringtone/1",
            )
            every { settingsRepository.settings } returns flowOf(settings)
            // 20:00 is outside the stored 09:00-17:00 window, which this mode must ignore, so a
            // DO_NOT_DISTURB outcome here also proves the window isn't being consulted.
            val scheduledAt = scheduledInstant(20, 0)

            val outcome = useCase(fixedNow = scheduledAt, doNotDisturbActive = true)
                .onAlarmFired(scheduledAt.toEpochMilli())

            assertEquals(AlarmFiredOutcome.DO_NOT_DISTURB, outcome)
            verify(exactly = 0) { alertPlayer.playVibration(any()) }
            verify(exactly = 0) { alertPlayer.playRingtone(any()) }
            verify { alarmScheduler.scheduleNext(scheduledInstant(20, 15).toEpochMilli()) }
        }

    @Test
    fun `following Do Not Disturb, an inactive filter fires whatever the time of day`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF,
            vibrationPattern = VibrationPatternType.LONG_PULSE,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(3, 0) // outside the stored window, which doesn't apply

        val outcome = useCase(fixedNow = scheduledAt, doNotDisturbActive = false)
            .onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.FIRED, outcome)
        verify { alertPlayer.playVibration(VibrationPatternType.LONG_PULSE) }
    }

    @Test
    fun `on custom times, Do Not Disturb is ignored and the alert still fires`() = runTest {
        // The two modes are a choice, not a combination: only the mode that opted in consults DND,
        // so custom times keeps firing through it exactly as it always has.
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            activeWindowMode = ActiveWindowMode.CUSTOM_TIMES,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt, doNotDisturbActive = true)
            .onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.FIRED, outcome)
        verify { alertPlayer.playVibration(VibrationPatternType.LONG_PULSE) }
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
        coVerify { scheduleStateRepository.setNextTriggerAtMillis(expectedNext) }
    }

    @Test
    fun `the interval randomness varies the next tick from the scheduled reference`() = runTest {
        // Keyed off the scheduled time as always, so the deviation displaces the *next* slot rather
        // than compounding with however late this tick happened to run.
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 10,
            intervalRandomness = IntervalRandomness.TEN_PERCENT,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        useCase(fixedNow = scheduledAt, random = PinnedRandom.Shortest)
            .onAlarmFired(scheduledAt.toEpochMilli())

        // 10 minutes less the full 10% deviation, i.e. 9 minutes.
        val expectedNext = scheduledAt.plusSeconds(9 * 60).toEpochMilli()
        verify { alarmScheduler.scheduleNext(expectedNext) }
        coVerify { scheduleStateRepository.setNextTriggerAtMillis(expectedNext) }
    }

    @Test
    fun `a tick that finds the reminder disabled clears the recorded trigger`() = runTest {
        every { settingsRepository.settings } returns flowOf(ReminderSettings(enabled = false))
        val scheduledAt = scheduledInstant(10, 0)

        useCase(fixedNow = scheduledAt).onAlarmFired(scheduledAt.toEpochMilli())

        coVerify { scheduleStateRepository.setNextTriggerAtMillis(null) }
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

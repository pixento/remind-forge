package nl.pixento.betterhabits.domain

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
import nl.pixento.betterhabits.alerting.AlertPlayer
import nl.pixento.betterhabits.alerting.CarConnectionMonitor
import nl.pixento.betterhabits.alerting.DoNotDisturbMonitor
import nl.pixento.betterhabits.data.ScheduleStateRepository
import nl.pixento.betterhabits.data.SettingsRepository
import nl.pixento.betterhabits.domain.model.IntervalRandomness
import nl.pixento.betterhabits.domain.model.ReminderSettings
import nl.pixento.betterhabits.domain.model.VibrationPatternType
import nl.pixento.betterhabits.scheduling.AlarmScheduler
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
        connectedToCar: Boolean = false,
        random: Random = Random.Default,
    ) =
        TriggerReminderUseCase(
            settingsRepository = settingsRepository,
            scheduleStateRepository = scheduleStateRepository,
            alertPlayer = alertPlayer,
            alarmScheduler = alarmScheduler,
            doNotDisturbMonitor = FakeDoNotDisturbMonitor(doNotDisturbActive),
            carConnectionMonitor = FakeCarConnectionMonitor(connectedToCar),
            zone = zone,
            now = { fixedNow },
            random = random,
        )

    private class FakeDoNotDisturbMonitor(private val active: Boolean) : DoNotDisturbMonitor {
        override fun isDoNotDisturbActive(): Boolean = active
    }

    private class FakeCarConnectionMonitor(private val connected: Boolean) : CarConnectionMonitor {
        override fun isConnectedToCar(): Boolean = connected
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
    fun `pausing for Do Not Disturb skips the alert but still reschedules`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            pauseDuringDoNotDisturb = true,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
            ringtoneUri = "content://media/ringtone/1",
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt, doNotDisturbActive = true)
            .onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.DO_NOT_DISTURB, outcome)
        verify(exactly = 0) { alertPlayer.playVibration(any()) }
        verify(exactly = 0) { alertPlayer.playRingtone(any()) }
        // The chain keeps its cadence, so it resumes the moment the condition clears.
        verify { alarmScheduler.scheduleNext(scheduledInstant(10, 15).toEpochMilli()) }
    }

    @Test
    fun `pausing for Android Auto skips the alert but still reschedules`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            pauseDuringAndroidAuto = true,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
            ringtoneUri = "content://media/ringtone/1",
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(fixedNow = scheduledAt, connectedToCar = true)
            .onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.ANDROID_AUTO, outcome)
        verify(exactly = 0) { alertPlayer.playVibration(any()) }
        verify(exactly = 0) { alertPlayer.playRingtone(any()) }
        verify { alarmScheduler.scheduleNext(scheduledInstant(10, 15).toEpochMilli()) }
    }

    @Test
    fun `a condition that wasn't opted into is never consulted`() = runTest {
        // Each pause is independent and off by default, so a phone in Do Not Disturb and plugged
        // into a car still buzzes for someone who asked for neither.
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        val scheduledAt = scheduledInstant(10, 0)

        val outcome = useCase(
            fixedNow = scheduledAt,
            doNotDisturbActive = true,
            connectedToCar = true,
        ).onAlarmFired(scheduledAt.toEpochMilli())

        assertEquals(AlarmFiredOutcome.FIRED, outcome)
        verify { alertPlayer.playVibration(VibrationPatternType.LONG_PULSE) }
    }

    @Test
    fun `the pause conditions combine with the active hours rather than replacing them`() = runTest {
        // The point of the whole arrangement: hours AND Do Not Disturb, not one or the other. Both
        // opted into, in-hours, and neither condition holding, so it fires.
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            pauseDuringDoNotDisturb = true,
            pauseDuringAndroidAuto = true,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
        )
        every { settingsRepository.settings } returns flowOf(settings)

        val inHours = scheduledInstant(10, 0)
        assertEquals(
            AlarmFiredOutcome.FIRED,
            useCase(fixedNow = inHours).onAlarmFired(inHours.toEpochMilli()),
        )

        // ... and the hours still apply on their own, even with both conditions clear.
        val outOfHours = scheduledInstant(20, 0)
        assertEquals(
            AlarmFiredOutcome.OUTSIDE_WINDOW,
            useCase(fixedNow = outOfHours).onAlarmFired(outOfHours.toEpochMilli()),
        )
    }

    @Test
    fun `without active hours a pause condition is still judged per tick`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            limitToActiveHours = false,
            pauseDuringDoNotDisturb = true,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
            vibrationPattern = VibrationPatternType.LONG_PULSE,
        )
        every { settingsRepository.settings } returns flowOf(settings)
        // 03:00 is outside the stored window, which doesn't apply here.
        val scheduledAt = scheduledInstant(3, 0)

        assertEquals(
            AlarmFiredOutcome.FIRED,
            useCase(fixedNow = scheduledAt, doNotDisturbActive = false)
                .onAlarmFired(scheduledAt.toEpochMilli()),
        )
        assertEquals(
            AlarmFiredOutcome.DO_NOT_DISTURB,
            useCase(fixedNow = scheduledAt, doNotDisturbActive = true)
                .onAlarmFired(scheduledAt.toEpochMilli()),
        )
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

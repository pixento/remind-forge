package nl.pixento.remindforge.domain

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.scheduling.AlarmScheduler
import org.junit.Test

class ReminderScheduleCoordinatorTest {

    private val zone = ZoneOffset.UTC
    private val settingsRepository = mockk<SettingsRepository>()
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)
    private val fixedNow = Instant.parse("2026-01-01T10:00:00Z")

    private fun coordinator() = ReminderScheduleCoordinator(
        settingsRepository = settingsRepository,
        alarmScheduler = alarmScheduler,
        zone = zone,
        now = { fixedNow },
    )

    @Test
    fun `healIfNeeded does nothing when a pending alarm already exists`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        every { settingsRepository.settings } returns flowOf(settings)
        every { alarmScheduler.hasPending() } returns true

        coordinator().healIfNeeded()

        verify(exactly = 0) { alarmScheduler.scheduleNext(any()) }
        verify(exactly = 0) { alarmScheduler.cancel() }
    }

    @Test
    fun `healIfNeeded reschedules when enabled but nothing is pending`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        every { settingsRepository.settings } returns flowOf(settings)
        every { alarmScheduler.hasPending() } returns false

        coordinator().healIfNeeded()

        coVerify { alarmScheduler.scheduleNext(any()) }
    }

    @Test
    fun `healIfNeeded does nothing when disabled`() = runTest {
        val settings = ReminderSettings(enabled = false)
        every { settingsRepository.settings } returns flowOf(settings)
        every { alarmScheduler.hasPending() } returns false

        coordinator().healIfNeeded()

        verify(exactly = 0) { alarmScheduler.scheduleNext(any()) }
        verify(exactly = 0) { alarmScheduler.cancel() }
    }

    @Test
    fun `rescheduleFromNow cancels when disabled`() = runTest {
        every { settingsRepository.settings } returns flowOf(ReminderSettings(enabled = false))

        coordinator().rescheduleFromNow()

        verify { alarmScheduler.cancel() }
        verify(exactly = 0) { alarmScheduler.scheduleNext(any()) }
    }

    @Test
    fun `rescheduleFromNow schedules from the current instant when enabled`() = runTest {
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        every { settingsRepository.settings } returns flowOf(settings)

        coordinator().rescheduleFromNow()

        val expectedNext = fixedNow.plusSeconds(15 * 60).toEpochMilli()
        verify { alarmScheduler.scheduleNext(expectedNext) }
    }
}

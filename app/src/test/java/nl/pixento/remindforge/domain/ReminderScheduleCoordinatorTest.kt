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
import nl.pixento.remindforge.data.ScheduleStateRepository
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.scheduling.AlarmScheduler
import org.junit.Before
import org.junit.Test

class ReminderScheduleCoordinatorTest {

    private val zone = ZoneOffset.UTC
    private val settingsRepository = mockk<SettingsRepository>()
    private val scheduleStateRepository = mockk<ScheduleStateRepository>(relaxUnitFun = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)
    private val fixedNow = Instant.parse("2026-01-01T10:00:00Z")

    @Before
    fun setUp() {
        every { scheduleStateRepository.nextTriggerAtMillis } returns flowOf(1L)
    }

    private fun coordinator() = ReminderScheduleCoordinator(
        settingsRepository = settingsRepository,
        scheduleStateRepository = scheduleStateRepository,
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
    fun `healIfNeeded restarts a pending chain whose trigger time was never recorded`() = runTest {
        // Upgrade from a version that didn't record it: the alarm fires at an unknowable time.
        val settings = ReminderSettings(
            enabled = true,
            intervalMinutes = 15,
            windowStart = LocalTime.of(9, 0),
            windowEnd = LocalTime.of(17, 0),
        )
        every { settingsRepository.settings } returns flowOf(settings)
        every { alarmScheduler.hasPending() } returns true
        every { scheduleStateRepository.nextTriggerAtMillis } returns flowOf(null)

        coordinator().healIfNeeded()

        verify { alarmScheduler.scheduleNext(fixedNow.plusSeconds(15 * 60).toEpochMilli()) }
    }

    @Test
    fun `rescheduleFromNow cancels when disabled`() = runTest {
        every { settingsRepository.settings } returns flowOf(ReminderSettings(enabled = false))

        coordinator().rescheduleFromNow()

        verify { alarmScheduler.cancel() }
        verify(exactly = 0) { alarmScheduler.scheduleNext(any()) }
        coVerify { scheduleStateRepository.setNextTriggerAtMillis(null) }
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
        coVerify { scheduleStateRepository.setNextTriggerAtMillis(expectedNext) }
    }

    @Test
    fun `following Do Not Disturb schedules the plain interval, ignoring the stored window`() =
        runTest {
            // Whether a future slot is active can't be known now, so this mode never clamps: the
            // window times are still stored but must not push the next tick to 09:00 tomorrow.
            val settings = ReminderSettings(
                enabled = true,
                intervalMinutes = 15,
                activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF,
                windowStart = LocalTime.of(1, 0),
                windowEnd = LocalTime.of(2, 0),
            )
            every { settingsRepository.settings } returns flowOf(settings)

            coordinator().rescheduleFromNow()

            verify { alarmScheduler.scheduleNext(fixedNow.plusSeconds(15 * 60).toEpochMilli()) }
        }
}

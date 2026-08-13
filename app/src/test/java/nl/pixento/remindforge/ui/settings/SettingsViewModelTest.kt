package nl.pixento.remindforge.ui.settings

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.pixento.remindforge.alerting.DoNotDisturbMonitor
import nl.pixento.remindforge.domain.ReminderScheduleCoordinator
import nl.pixento.remindforge.domain.model.ActiveWindowMode
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.BatteryOptimization
import nl.pixento.remindforge.scheduling.ExactAlarmPermission
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeSettingsRepository
    private lateinit var scheduleState: FakeScheduleStateRepository
    private lateinit var coordinator: ReminderScheduleCoordinator
    private lateinit var context: Context
    private lateinit var doNotDisturbMonitor: DoNotDisturbMonitor

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeSettingsRepository()
        scheduleState = FakeScheduleStateRepository()
        coordinator = mockk(relaxed = true)
        context = mockk(relaxed = true)
        doNotDisturbMonitor = mockk()
        every { doNotDisturbMonitor.isDoNotDisturbActive() } returns false

        mockkObject(ExactAlarmPermission)
        every { ExactAlarmPermission.canScheduleExactAlarms(any()) } returns true

        mockkObject(BatteryOptimization)
        every { BatteryOptimization.isIgnoringBatteryOptimizations(any()) } returns true

        mockkStatic(RingtoneManager::class)
        every { RingtoneManager.getRingtone(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun viewModel() =
        SettingsViewModel(context, repository, scheduleState, coordinator, doNotDisturbMonitor)

    @Test
    fun `initial state mirrors repository defaults`() {
        val vm = viewModel()
        assertEquals(ReminderSettings().intervalMinutes, vm.uiState.value.intervalMinutes)
        assertEquals(ReminderSettings().enabled, vm.uiState.value.enabled)
    }

    @Test
    fun `init triggers a coordinator heal check`() {
        viewModel()
        coVerify { coordinator.healIfNeeded() }
    }

    @Test
    fun `enabling triggers a coordinator reschedule`() = runTest {
        val vm = viewModel()
        vm.onEnabledChanged(true)
        assertTrue(vm.uiState.value.enabled)
        coVerify { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `disabling triggers a coordinator reschedule`() = runTest {
        repository = FakeSettingsRepository(ReminderSettings(enabled = true))
        val vm = viewModel()
        vm.onEnabledChanged(false)
        assertFalse(vm.uiState.value.enabled)
        coVerify { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `changing the interval or window triggers a coordinator reschedule`() = runTest {
        val vm = viewModel()

        vm.onIntervalChanged(ReminderSettings().intervalMinutes + 5)
        vm.onWindowStartChanged(ReminderSettings().windowStart.plusHours(1))
        vm.onWindowEndChanged(ReminderSettings().windowEnd.plusHours(1))

        coVerify(exactly = 3) { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `changing the active window mode triggers a coordinator reschedule`() = runTest {
        val vm = viewModel()

        vm.onActiveWindowModeChanged(ActiveWindowMode.DO_NOT_DISTURB_OFF)

        assertEquals(ActiveWindowMode.DO_NOT_DISTURB_OFF, vm.uiState.value.activeWindowMode)
        coVerify(exactly = 1) { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `re-picking the same active window mode leaves the running chain alone`() = runTest {
        repository = FakeSettingsRepository(ReminderSettings(enabled = true))
        val vm = viewModel()

        vm.onActiveWindowModeChanged(ActiveWindowMode.CUSTOM_TIMES)

        coVerify(exactly = 0) { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `reminders read as paused only while following Do Not Disturb and it is on`() = runTest {
        every { doNotDisturbMonitor.isDoNotDisturbActive() } returns true
        repository = FakeSettingsRepository(ReminderSettings(enabled = true))
        val vm = viewModel()

        // Enabled and DND is on, but this mode doesn't consult it.
        assertFalse(vm.uiState.value.remindersPausedByDoNotDisturb)

        vm.onActiveWindowModeChanged(ActiveWindowMode.DO_NOT_DISTURB_OFF)
        assertTrue(vm.uiState.value.remindersPausedByDoNotDisturb)

        vm.onEnabledChanged(false)
        assertFalse(vm.uiState.value.remindersPausedByDoNotDisturb)
    }

    @Test
    fun `resume check picks up Do Not Disturb being turned off elsewhere`() = runTest {
        // The filter is changed outside this app - system settings, the quick-settings tile, a
        // schedule ending - so coming back to the screen is when it gets re-read.
        every { doNotDisturbMonitor.isDoNotDisturbActive() } returns true
        repository = FakeSettingsRepository(
            ReminderSettings(enabled = true, activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF),
        )
        val vm = viewModel()
        assertTrue(vm.uiState.value.remindersPausedByDoNotDisturb)

        every { doNotDisturbMonitor.isDoNotDisturbActive() } returns false
        vm.onExactAlarmPermissionResumeCheck()

        assertFalse(vm.uiState.value.remindersPausedByDoNotDisturb)
    }

    @Test
    fun `alert channel changes leave the running chain alone`() = runTest {
        repository = FakeSettingsRepository(ReminderSettings(enabled = true))
        val vm = viewModel()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/ringtone/42"

        vm.onVibrationPatternSelected(VibrationPatternType.TRIPLE_PULSE)
        vm.onRingtoneSelected(uri)

        coVerify(exactly = 0) { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `re-picking the same interval leaves the running chain alone`() = runTest {
        repository = FakeSettingsRepository(ReminderSettings(enabled = true))
        val vm = viewModel()

        vm.onIntervalChanged(ReminderSettings().intervalMinutes)

        coVerify(exactly = 0) { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `the pending alarm's trigger time is surfaced in state`() = runTest {
        scheduleState = FakeScheduleStateRepository(initial = 1_800_000L)
        val vm = viewModel()
        assertEquals(1_800_000L, vm.uiState.value.nextTriggerAtMillis)
    }

    @Test
    fun `vibration pattern selection updates state`() {
        val vm = viewModel()
        vm.onVibrationPatternSelected(VibrationPatternType.TRIPLE_PULSE)
        assertEquals(VibrationPatternType.TRIPLE_PULSE, vm.uiState.value.vibrationPattern)
    }

    @Test
    fun `ringtone selection persists the uri string`() {
        val vm = viewModel()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/ringtone/42"

        vm.onRingtoneSelected(uri)

        assertEquals("content://media/ringtone/42", vm.uiState.value.ringtoneUri)
    }

    @Test
    fun `selecting the silent ringtone clears the stored uri`() {
        val vm = viewModel()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/ringtone/42"
        vm.onRingtoneSelected(uri)

        vm.onRingtoneSelected(null)

        assertNull(vm.uiState.value.ringtoneUri)
    }

    @Test
    fun `no alert is flagged only when both channels are silent`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.hasNoAlertSelected)

        vm.onVibrationPatternSelected(VibrationPatternType.SILENT)
        assertTrue(vm.uiState.value.hasNoAlertSelected)

        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/ringtone/42"
        vm.onRingtoneSelected(uri)
        assertFalse(vm.uiState.value.hasNoAlertSelected)
    }

    @Test
    fun `resume check re-evaluates exact alarm permission after it is granted`() {
        every { ExactAlarmPermission.canScheduleExactAlarms(any()) } returns false
        val vm = viewModel()
        assertTrue(vm.uiState.value.needsExactAlarmPermission)

        every { ExactAlarmPermission.canScheduleExactAlarms(any()) } returns true
        vm.onExactAlarmPermissionResumeCheck()

        assertFalse(vm.uiState.value.needsExactAlarmPermission)
    }

    @Test
    fun `battery optimization banner shown when app is not exempt`() {
        every { BatteryOptimization.isIgnoringBatteryOptimizations(any()) } returns false
        val vm = viewModel()
        assertTrue(vm.uiState.value.showBatteryOptimizationBanner)
    }

    @Test
    fun `battery optimization banner hidden when app is already exempt`() {
        every { BatteryOptimization.isIgnoringBatteryOptimizations(any()) } returns true
        val vm = viewModel()
        assertFalse(vm.uiState.value.showBatteryOptimizationBanner)
    }

    @Test
    fun `dismissing battery optimization banner hides it and survives a resume check`() {
        every { BatteryOptimization.isIgnoringBatteryOptimizations(any()) } returns false
        val vm = viewModel()
        assertTrue(vm.uiState.value.showBatteryOptimizationBanner)

        vm.onDismissBatteryOptimizationBanner()
        assertFalse(vm.uiState.value.showBatteryOptimizationBanner)

        vm.onExactAlarmPermissionResumeCheck()
        assertFalse(vm.uiState.value.showBatteryOptimizationBanner)
    }
}

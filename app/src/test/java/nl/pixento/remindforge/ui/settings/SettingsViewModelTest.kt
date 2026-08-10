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
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.pixento.remindforge.alerting.AlertPlayer
import nl.pixento.remindforge.domain.ReminderScheduleCoordinator
import nl.pixento.remindforge.domain.model.AlertMode
import nl.pixento.remindforge.domain.model.ReminderSettings
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.scheduling.BatteryOptimization
import nl.pixento.remindforge.scheduling.ExactAlarmPermission
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeSettingsRepository
    private lateinit var coordinator: ReminderScheduleCoordinator
    private lateinit var alertPlayer: AlertPlayer
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeSettingsRepository()
        coordinator = mockk(relaxed = true)
        alertPlayer = mockk(relaxUnitFun = true)
        context = mockk(relaxed = true)

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

    private fun viewModel() = SettingsViewModel(context, repository, coordinator, alertPlayer)

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
    fun `interval below minimum is clamped to 5`() {
        val vm = viewModel()
        vm.onIntervalChanged(2)
        assertEquals(5, vm.uiState.value.intervalMinutes)
    }

    @Test
    fun `interval above maximum is clamped to 30`() {
        val vm = viewModel()
        vm.onIntervalChanged(100)
        assertEquals(30, vm.uiState.value.intervalMinutes)
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
        val vm = viewModel()
        vm.onEnabledChanged(false)
        assertFalse(vm.uiState.value.enabled)
        coVerify { coordinator.rescheduleFromNow() }
    }

    @Test
    fun `alert mode switch updates state`() {
        val vm = viewModel()
        assertEquals(AlertMode.VIBRATION, vm.uiState.value.alertMode)

        vm.onAlertModeChanged(AlertMode.RINGTONE)

        assertEquals(AlertMode.RINGTONE, vm.uiState.value.alertMode)
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
    fun `preview vibration delegates to AlertPlayer with the given pattern`() {
        val vm = viewModel()
        vm.onPreviewVibration(VibrationPatternType.DOUBLE_PULSE)
        verify { alertPlayer.playVibration(VibrationPatternType.DOUBLE_PULSE) }
    }

    @Test
    fun `preview ringtone delegates to AlertPlayer with the given uri`() {
        val vm = viewModel()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://media/ringtone/7"

        vm.onPreviewRingtone(uri)

        verify { alertPlayer.playRingtone("content://media/ringtone/7") }
    }

    @Test
    fun `missing exact alarm permission is reflected in state`() {
        every { ExactAlarmPermission.canScheduleExactAlarms(any()) } returns false
        val vm = viewModel()
        assertTrue(vm.uiState.value.needsExactAlarmPermission)
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

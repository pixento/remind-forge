package nl.pixento.betterhabits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import nl.pixento.betterhabits.ui.settings.SettingsRoute
import nl.pixento.betterhabits.ui.settings.SettingsViewModel
import nl.pixento.betterhabits.ui.theme.BetterHabitsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as BetterHabitsApplication).container

        setContent {
            BetterHabitsTheme {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SettingsViewModel(
                                appContext = applicationContext,
                                settingsRepository = container.settingsRepository,
                                scheduleStateRepository = container.scheduleStateRepository,
                                scheduleCoordinator = container.scheduleCoordinator,
                                doNotDisturbMonitor = container.doNotDisturbMonitor,
                            )
                        }
                    },
                )

                // Permission grants and Do Not Disturb are all changed outside this app, so
                // re-read the device state whenever the user comes back to it.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.onExactAlarmPermissionResumeCheck()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // safeDrawing rather than the Scaffold default: enableEdgeToEdge() opts the
                // window into LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS, so landscape on a notched
                // device needs the cutout kept clear too, not just the system bars. The padding
                // goes to the screen as content padding - see PaddingValues.plus.
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                ) { innerPadding ->
                    SettingsRoute(viewModel = viewModel, contentPadding = innerPadding)
                }
            }
        }
    }
}

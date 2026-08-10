package nl.pixento.remindforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import nl.pixento.remindforge.ui.settings.SettingsRoute
import nl.pixento.remindforge.ui.settings.SettingsViewModel
import nl.pixento.remindforge.ui.theme.RemindForgeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RemindForgeApplication).container

        setContent {
            RemindForgeTheme {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SettingsViewModel(
                                appContext = applicationContext,
                                settingsRepository = container.settingsRepository,
                                scheduleCoordinator = container.scheduleCoordinator,
                                alertPlayer = container.alertPlayer,
                            )
                        }
                    },
                )

                // The exact-alarm grant happens in a system settings screen, so re-check on resume.
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsRoute(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

package nl.pixento.betterhabits.ui.settings.vibration

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import nl.pixento.betterhabits.BetterHabitsApplication
import nl.pixento.betterhabits.domain.model.ReminderSettings
import nl.pixento.betterhabits.domain.model.VibrationPatternType
import nl.pixento.betterhabits.ui.theme.BetterHabitsTheme

/**
 * The vibration counterpart of the system ringtone picker: its own window, so both rows of the
 * alert settings group open the same kind of screen.
 *
 * It deliberately doesn't write the setting itself - every settings write in the app goes through
 * [nl.pixento.betterhabits.ui.settings.SettingsViewModel], which reschedules the alarm chain after
 * each one. Instead each tap updates the activity result, which is delivered when the screen
 * finishes (back arrow or system back), and the caller persists it once.
 */
class VibrationPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val alertPlayer = (application as BetterHabitsApplication).container.alertPlayer
        val initial = intent.getStringExtra(EXTRA_INITIAL_PATTERN).toPatternOrDefault()

        setContent {
            BetterHabitsTheme {
                // Stored by name so it survives process death along with the rest of the instance state.
                var selectedName by rememberSaveable { mutableStateOf(initial.name) }

                VibrationPatternScreen(
                    selected = selectedName.toPatternOrDefault(),
                    onSelect = { pattern ->
                        selectedName = pattern.name
                        // Audition the choice the way the ringtone picker plays a sound on tap.
                        alertPlayer.playVibration(pattern)
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_PICKED_PATTERN, pattern.name),
                        )
                    },
                    onBack = { finish() },
                )
            }
        }
    }
}

internal const val EXTRA_INITIAL_PATTERN = "initial_pattern"
internal const val EXTRA_PICKED_PATTERN = "picked_pattern"

private fun String?.toPatternOrDefault(): VibrationPatternType =
    this?.let { name -> VibrationPatternType.entries.firstOrNull { it.name == name } }
        ?: ReminderSettings().vibrationPattern

/**
 * Launches [VibrationPickerActivity], the way the Sound row launches the system ringtone picker.
 *
 * A null result means the screen was left without picking anything, i.e. no change - unlike the
 * ringtone picker's null "Silent", since silencing vibration is [VibrationPatternType.SILENT], an
 * ordinary pick.
 */
object PickVibrationPattern :
    ActivityResultContract<VibrationPatternType, VibrationPatternType?>() {

    override fun createIntent(context: Context, input: VibrationPatternType): Intent =
        Intent(context, VibrationPickerActivity::class.java)
            .putExtra(EXTRA_INITIAL_PATTERN, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): VibrationPatternType? {
        if (resultCode != Activity.RESULT_OK) return null
        val name = intent?.getStringExtra(EXTRA_PICKED_PATTERN) ?: return null
        return VibrationPatternType.entries.firstOrNull { it.name == name }
    }
}

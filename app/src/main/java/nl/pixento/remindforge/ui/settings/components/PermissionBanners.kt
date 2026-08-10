package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExactAlarmPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Reminders need permission to schedule exact alarms so they stay on time.")
            TextButton(onClick = onRequestPermission) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
fun BatteryOptimizationBanner(
    onRequestExemption: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Some phones aggressively kill scheduled reminders to save battery. " +
                        "Exempting this app keeps reminders reliable (optional).",
            )
            TextButton(onClick = onRequestExemption) {
                Text("Open battery settings")
            }
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    }
}

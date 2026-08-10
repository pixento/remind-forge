package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.pixento.remindforge.domain.model.ReminderSettings

@Composable
fun IntervalStepper(
    intervalMinutes: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 5,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Button(onClick = {
            onIntervalChange((intervalMinutes - step).coerceAtLeast(ReminderSettings.MIN_INTERVAL_MINUTES))
        }) {
            Text("-")
        }
        Text("$intervalMinutes min")
        Button(onClick = {
            onIntervalChange((intervalMinutes + step).coerceAtMost(ReminderSettings.MAX_INTERVAL_MINUTES))
        }) {
            Text("+")
        }
    }
}

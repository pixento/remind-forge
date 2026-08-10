package nl.pixento.remindforge.ui.settings.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalTime

@Composable
fun TimeWindowPicker(
    windowStart: LocalTime,
    windowEnd: LocalTime,
    onWindowStartChange: (LocalTime) -> Unit,
    onWindowEndChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onWindowStartChange(LocalTime.of(hour, minute)) },
                    windowStart.hour,
                    windowStart.minute,
                    true,
                ).show()
            }) {
                Text("Start: %02d:%02d".format(windowStart.hour, windowStart.minute))
            }
            OutlinedButton(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onWindowEndChange(LocalTime.of(hour, minute)) },
                    windowEnd.hour,
                    windowEnd.minute,
                    true,
                ).show()
            }) {
                Text("End: %02d:%02d".format(windowEnd.hour, windowEnd.minute))
            }
        }
        if (windowStart > windowEnd) {
            Text("Overnight window (wraps past midnight)")
        }
    }
}

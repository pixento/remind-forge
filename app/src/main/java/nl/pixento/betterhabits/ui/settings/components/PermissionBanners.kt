package nl.pixento.betterhabits.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.pixento.betterhabits.R

/**
 * Banners are the same grey block as the settings groups rather than a lighter card of their own -
 * the two notices that *do* need to stand out (Do Not Disturb paused, no alert selected) carry an
 * accent container for that, so a third fill here would only make the page look patchy. The accent
 * action buttons are what draw the eye instead.
 */
private val bannerColors
    @Composable get() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

@Composable
fun ExactAlarmPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth(), shape = SettingsCardShape, colors = bannerColors) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.exact_alarm_banner_message))
            TextButton(onClick = onRequestPermission) {
                Text(stringResource(R.string.exact_alarm_banner_action))
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
    Card(modifier = modifier.fillMaxWidth(), shape = SettingsCardShape, colors = bannerColors) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.battery_optimization_banner_message))
            TextButton(onClick = onRequestExemption) {
                Text(stringResource(R.string.battery_optimization_banner_action))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.battery_optimization_banner_dismiss))
            }
        }
    }
}

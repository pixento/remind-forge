package nl.pixento.betterhabits.ui.settings.vibration

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.pixento.betterhabits.R
import nl.pixento.betterhabits.domain.model.VibrationPatternType
import nl.pixento.betterhabits.ui.settings.components.SettingsDivider
import nl.pixento.betterhabits.ui.settings.components.SettingsGroup
import nl.pixento.betterhabits.ui.settings.components.SettingsRow
import nl.pixento.betterhabits.ui.settings.components.plus

/**
 * Full-screen pattern picker, shaped like the system ringtone picker the Sound row opens: a titled
 * bar with a back arrow over a grouped card of radio rows. Tapping a row commits it right away (and
 * auditions it) - there is no Cancel/OK, leaving the screen is the whole confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationPatternScreen(
    selected: VibrationPatternType,
    onSelect: (VibrationPatternType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Matches MainActivity: the cutout has to be kept clear in landscape as well as the bars.
        // The top bar consumes the status bar inset itself, so what reaches innerPadding on top is
        // the bar's height.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vibration_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.vibration_picker_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
            contentPadding = innerPadding.plus(PaddingValues(16.dp)),
        ) {
            item {
                SettingsGroup {
                    VibrationPatternType.entries.forEachIndexed { index, pattern ->
                        if (index > 0) SettingsDivider()
                        SettingsRow(
                            title = pattern.label,
                            onClick = { onSelect(pattern) },
                            selected = pattern == selected,
                            leading = {
                                RadioButton(selected = pattern == selected, onClick = null)
                            },
                        )
                    }
                }
            }
        }
    }
}

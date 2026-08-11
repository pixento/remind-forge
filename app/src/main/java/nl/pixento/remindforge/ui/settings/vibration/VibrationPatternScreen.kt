package nl.pixento.remindforge.ui.settings.vibration

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.ui.settings.components.SettingsDivider
import nl.pixento.remindforge.ui.settings.components.SettingsGroup
import nl.pixento.remindforge.ui.settings.components.SettingsRow

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
        topBar = {
            TopAppBar(
                title = { Text("Vibration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .selectableGroup(),
            contentPadding = PaddingValues(16.dp),
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

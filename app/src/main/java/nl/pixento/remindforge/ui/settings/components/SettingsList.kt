package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The three primitives every settings section is built from, so rows stay identical by
 * construction: a rounded card grouping related rows, a row that shows its current value on a
 * second line, and the inset divider between rows. Modelled on the platform sound-and-vibration
 * settings screens users already know - the whole row is the tap target and opens a picker, rather
 * than each setting carrying its own bespoke inline control.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(content = content)
    }
}

/**
 * A settings row. [selected] turns the row into a radio-list entry: pass it (together with a
 * [leading] `RadioButton`) so the row is announced as a selectable option rather than a button, the
 * way the full-screen pickers use it.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val clickModifier = when {
        onClick == null -> Modifier
        selected != null -> Modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        )
        else -> Modifier.clickable(onClick = onClick)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Title + short description shown above a [SettingsGroup] card, naming what its rows are for -
 * the platform Settings idiom of a section header sitting outside the grouped card rather than
 * inside it.
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

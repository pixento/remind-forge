package nl.pixento.remindforge.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The three primitives every settings section is built from, so rows stay identical by
 * construction: a rounded card grouping related rows, a row that shows its current value on a
 * second line, and the inset divider between rows. Modelled on the platform sound-and-vibration
 * settings screens users already know - the whole row is the tap target and opens a picker, rather
 * than each setting carrying its own bespoke inline control.
 */
/**
 * Shared by every card on the settings screens - the grouped rows below as well as the notices and
 * permission banners between them - so they read as one family rather than as cards of two radii.
 */
val SettingsCardShape = RoundedCornerShape(16.dp)

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(content = content)
    }
}

/**
 * A settings row.
 *
 * [selected] turns the row into a radio-list entry and [checked] into a checkbox one: pass either
 * (together with the matching [leading] or [trailing] control) so the row is announced as that kind
 * of option rather than as a plain button.
 *
 * [enabled] `false` dims the row and stops it responding, for a setting that is still worth showing
 * but doesn't currently apply. It keeps the click modifier so the row is *announced* as disabled
 * rather than just silently inert.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean? = null,
    checked: Boolean? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val clickModifier = when {
        onClick == null -> Modifier
        checked != null -> Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        )
        selected != null -> Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        )
        else -> Modifier.clickable(enabled = enabled, onClick = onClick)
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
                color = MaterialTheme.colorScheme.onSurface.applyIfDisabled(enabled),
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.applyIfDisabled(enabled),
                )
            }
        }
        trailing?.invoke()
    }
}

/** Material 3's disabled-content opacity, so a dimmed row matches the platform's own. */
private fun Color.applyIfDisabled(enabled: Boolean): Color =
    if (enabled) this else copy(alpha = 0.38f)

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

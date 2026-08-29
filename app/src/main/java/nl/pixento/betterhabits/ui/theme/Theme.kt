package nl.pixento.betterhabits.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PastelRed80,
    onPrimary = OnPastelRed80,
    primaryContainer = PastelRedContainer80,
    onPrimaryContainer = OnPastelRedContainer80,
    secondary = PastelRedGrey80,
    onSecondary = OnPastelRedGrey80,
    secondaryContainer = PastelRedGreyContainer80,
    onSecondaryContainer = OnPastelRedGreyContainer80,
    tertiary = Clay80,
    onTertiary = OnClay80,
    tertiaryContainer = ClayContainer80,
    onTertiaryContainer = OnClayContainer80,
)

private val LightColorScheme = lightColorScheme(
    primary = PastelRed40,
    onPrimary = OnPastelRed40,
    primaryContainer = PastelRedContainer40,
    onPrimaryContainer = OnPastelRedContainer40,
    secondary = PastelRedGrey40,
    onSecondary = OnPastelRedGrey40,
    secondaryContainer = PastelRedGreyContainer40,
    onSecondaryContainer = OnPastelRedGreyContainer40,
    tertiary = Clay40,
    onTertiary = OnClay40,
    tertiaryContainer = ClayContainer40,
    onTertiaryContainer = OnClayContainer40,
)

@Composable
fun BetterHabitsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }.withNeutralSurfaces(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Replaces the scheme's surface roles with fixed neutral greys, so the grouped settings cards sit as
 * flat blocks on a near-black (or near-white) page on every device instead of being tinted the same
 * hue as the page by Material You.
 *
 * Applied to *both* branches above, dynamic and fallback alike, so the screen looks the same either
 * way. The accent roles - primary/secondary/tertiary and the `*Container` pairs the Do Not Disturb
 * and no-alert notices are painted with - are deliberately left dynamic, which is what still ties the
 * value lines and switches to the user's wallpaper.
 */
private fun ColorScheme.withNeutralSurfaces(darkTheme: Boolean): ColorScheme =
    if (darkTheme) {
        copy(
            background = NeutralDarkBackground,
            onBackground = NeutralDarkOnSurface,
            surface = NeutralDarkBackground,
            onSurface = NeutralDarkOnSurface,
            surfaceVariant = NeutralDarkSurfaceVariant,
            onSurfaceVariant = NeutralDarkOnSurfaceVariant,
            surfaceContainerLowest = NeutralDarkSurfaceContainerLowest,
            surfaceContainerLow = NeutralDarkSurfaceContainerLow,
            surfaceContainer = NeutralDarkSurfaceContainer,
            surfaceContainerHigh = NeutralDarkSurfaceContainerHigh,
            surfaceContainerHighest = NeutralDarkSurfaceContainerHighest,
            outline = NeutralDarkOutline,
            outlineVariant = NeutralDarkOutlineVariant,
            // The greys are already the finished colour; an elevation overlay would re-tint them.
            surfaceTint = Color.Transparent,
        )
    } else {
        copy(
            background = NeutralLightBackground,
            onBackground = NeutralLightOnSurface,
            surface = NeutralLightBackground,
            onSurface = NeutralLightOnSurface,
            surfaceVariant = NeutralLightSurfaceVariant,
            onSurfaceVariant = NeutralLightOnSurfaceVariant,
            surfaceContainerLowest = NeutralLightSurfaceContainerLowest,
            surfaceContainerLow = NeutralLightSurfaceContainerLow,
            surfaceContainer = NeutralLightSurfaceContainer,
            surfaceContainerHigh = NeutralLightSurfaceContainerHigh,
            surfaceContainerHighest = NeutralLightSurfaceContainerHighest,
            outline = NeutralLightOutline,
            outlineVariant = NeutralLightOutlineVariant,
            surfaceTint = Color.Transparent,
        )
    }
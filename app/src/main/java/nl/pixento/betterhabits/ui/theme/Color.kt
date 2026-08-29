package nl.pixento.betterhabits.ui.theme

import androidx.compose.ui.graphics.Color

// The brand accent: a dark pastel red - dusty and desaturated rather than a signal red. The 80s are
// the dark theme's roles (a light tone has to carry on a near-black page) and the 40s the light
// theme's; both are deep for their tone so the red reads as muted, not candied.
//
// The `on*` and `*Container` partners are spelled out rather than left to `darkColorScheme()`'s
// defaults: those defaults are Material's baseline *purple*, which the old accent happened to match,
// so leaving them out puts a purple switch thumb on a red track.
val PastelRed80 = Color(0xFFD98A85)
val OnPastelRed80 = Color(0xFF5A211D)
val PastelRedContainer80 = Color(0xFF763A35)
val OnPastelRedContainer80 = Color(0xFFFFDAD4)

val PastelRedGrey80 = Color(0xFFD8BFBD)
val OnPastelRedGrey80 = Color(0xFF402A28)
val PastelRedGreyContainer80 = Color(0xFF58403E)
val OnPastelRedGreyContainer80 = Color(0xFFFFDAD6)

val Clay80 = Color(0xFFE3B3A4)
val OnClay80 = Color(0xFF4A2519)
val ClayContainer80 = Color(0xFF643A2C)
val OnClayContainer80 = Color(0xFFFFDBCE)

val PastelRed40 = Color(0xFF8E4B47)
val OnPastelRed40 = Color(0xFFFFFFFF)
val PastelRedContainer40 = Color(0xFFFFDAD4)
val OnPastelRedContainer40 = Color(0xFF3B0A08)

val PastelRedGrey40 = Color(0xFF755654)
val OnPastelRedGrey40 = Color(0xFFFFFFFF)
val PastelRedGreyContainer40 = Color(0xFFFFDAD6)
val OnPastelRedGreyContainer40 = Color(0xFF2C1513)

val Clay40 = Color(0xFF7A5245)
val OnClay40 = Color(0xFFFFFFFF)
val ClayContainer40 = Color(0xFFFFDBCE)
val OnClayContainer40 = Color(0xFF2E1508)

/**
 * Fixed neutral surfaces, applied over whatever scheme `BetterHabitsTheme` picked (see
 * `ColorScheme.withNeutralSurfaces`). The settings screen is modelled on the platform
 * sound-and-vibration screen, where grouped cards read as flat grey blocks on a near-black page;
 * the wallpaper-derived surfaces of Material You tint page and card alike and blur that separation.
 * Only the surfaces are pinned - the accent roles stay dynamic.
 */
val NeutralDarkBackground = Color(0xFF0B0B0B)
val NeutralDarkSurfaceContainerLowest = Color(0xFF0F0F10)
val NeutralDarkSurfaceContainerLow = Color(0xFF161618)
val NeutralDarkSurfaceContainer = Color(0xFF1C1C1E)
val NeutralDarkSurfaceContainerHigh = Color(0xFF242426)
val NeutralDarkSurfaceContainerHighest = Color(0xFF2C2C2E)
val NeutralDarkSurfaceVariant = Color(0xFF242426)
val NeutralDarkOutline = Color(0xFF6E6E72)
val NeutralDarkOutlineVariant = Color(0xFF2E2E30)
val NeutralDarkOnSurface = Color(0xFFE6E6E6)
val NeutralDarkOnSurfaceVariant = Color(0xFFA8A8AA)

val NeutralLightBackground = Color(0xFFF2F2F6)
val NeutralLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val NeutralLightSurfaceContainerLow = Color(0xFFFAFAFC)
val NeutralLightSurfaceContainer = Color(0xFFFFFFFF)
val NeutralLightSurfaceContainerHigh = Color(0xFFF7F7FA)
val NeutralLightSurfaceContainerHighest = Color(0xFFEFEFF3)
val NeutralLightSurfaceVariant = Color(0xFFE7E7EC)
val NeutralLightOutline = Color(0xFF8C8C90)
val NeutralLightOutlineVariant = Color(0xFFE0E0E4)
val NeutralLightOnSurface = Color(0xFF1B1B1D)
val NeutralLightOnSurfaceVariant = Color(0xFF5A5A5E)
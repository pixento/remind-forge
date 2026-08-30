package nl.pixento.betterhabits.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.pixento.betterhabits.R
import nl.pixento.betterhabits.ui.theme.BetterHabitsTheme
import nl.pixento.betterhabits.ui.theme.NeutralDarkBackground
import nl.pixento.betterhabits.ui.theme.PastelRed80

/**
 * The branded background every listing graphic sits on: a top tinted towards the accent red,
 * settling into the same near-black the settings screen uses, so a screenshot inset melts into the
 * page rather than floating on a contrasting slab.
 */
private val FrameTop = Color(0xFF3A2120)
private val FrameBottom = NeutralDarkBackground

private val CaptionStyle = TextStyle(
    color = Color.White,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Medium,
    textAlign = TextAlign.Center,
)

/**
 * A phone screenshot: a caption over an inset of the real app.
 *
 * The caption band is a fixed height so the inset starts at the same y in every shot and every
 * language - a band that grew with a longer German caption would make the set look ragged when Play
 * shows the screenshots side by side.
 */
@Composable
internal fun StoreScreenshotFrame(caption: String, content: @Composable () -> Unit) {
    StoreGraphicBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.height(96.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = caption, style = CaptionStyle)
            }
            Spacer(Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = NeutralDarkBackground,
                shadowElevation = 20.dp,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                content()
            }
        }
    }
}

/** The 1024x500 feature graphic: the app's name over the locale's tagline, with the icon's mark. */
@Composable
internal fun StoreFeatureGraphic(appName: String, tagline: String) {
    // Diagonal rather than the screenshots' vertical gradient: over a banner only 250dp tall a
    // vertical ramp bottoms out halfway down and leaves the lower half a dead black band.
    StoreGraphicBackground(Brush.linearGradient(listOf(FrameTop, FrameBottom))) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 40.dp)) {
            // Deliberately bled off the right edge, and dim, so it reads as a backdrop rather than
            // a second logo competing with the name. Tinted to the accent rather than left in the
            // icon's own blues, which would clash with the red ground and read as a grey smudge.
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                colorFilter = ColorFilter.tint(PastelRed80),
                alpha = 0.20f,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .requiredSize(380.dp)
                    .offset(x = 150.dp),
            )
            Column(
                // Bounded so a long tagline wraps inside the left-hand column instead of running
                // into the mark - German and French are both longer than the English.
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.72f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = appName,
                    style = CaptionStyle.copy(
                        fontSize = 40.sp,
                        lineHeight = 46.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                    ),
                )
                Text(
                    text = tagline,
                    style = CaptionStyle.copy(
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Start,
                    ),
                )
            }
        }
    }
}

/**
 * The 512x512 Play icon, drawn from the very vectors the launcher uses
 * (`mipmap-anydpi-v26/ic_launcher.xml`) rather than upscaled from the 192px `mipmap-xxxhdpi` PNG.
 *
 * The 1.5x scale is the adaptive icon's own 108dp -> 72dp crop, so the mark sits at the size people
 * actually see on their home screen. That's only safe because the background layer is a flat opaque
 * fill with no artwork near the edges to lose. Deliberately *not* rounded or circle-clipped: Play
 * masks the icon itself, and a pre-masked one comes out double-rounded.
 */
@Composable
internal fun StoreAppIcon() {
    Box(modifier = Modifier.fillMaxSize().scale(1.5f)) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StoreGraphicBackground(
    brush: Brush = Brush.verticalGradient(listOf(FrameTop, FrameBottom)),
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(brush).clipToBounds()) {
        content()
    }
}

/**
 * The app as `MainActivity` puts it on screen, minus the wallpaper palette.
 *
 * `dynamicColor = false` matters: the default is true, and on the SDK Robolectric runs the dynamic
 * branch resolves the platform's own accent palette instead of the PastelRed80-over-neutral-greys look
 * the app actually ships to anyone without Material You.
 *
 * The `Scaffold` and its insets have to keep matching `MainActivity`'s, and the padding has to
 * reach the screen the same way - as its content padding - or the renders stop being a picture of
 * what the app does. Robolectric reports no system bar insets, so what that padding measures here
 * is zero; it is the shape of the wiring that is being kept honest, not the pixels.
 */
@Composable
internal fun AppScreen(content: @Composable (PaddingValues) -> Unit) {
    AppWindow {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { innerPadding -> content(innerPadding) }
    }
}

/**
 * The themed window with no scaffold, as `VibrationPickerActivity` sets it up - that screen brings
 * its own `Scaffold`, and wrapping it in a second one would pad it twice.
 */
@Composable
internal fun AppWindow(content: @Composable () -> Unit) {
    BetterHabitsTheme(darkTheme = true, dynamicColor = false) { content() }
}

/**
 * Shows a band of a screen that the inset viewport is too short to reach.
 *
 * The settings screen is a `LazyColumn`, so an item below the fold is never composed and simply
 * cropping wouldn't reveal it. Giving the child a viewport tall enough for the whole screen composes
 * everything, sliding it up picks the band, and the clip keeps it inside the inset. That avoids
 * needing a `LazyListState` the screen doesn't expose - and avoids a compose test rule, which this
 * source set doesn't have.
 */
@Composable
internal fun ScrolledTo(
    offsetDp: Int,
    fullHeightDp: Int = 1400,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // wrapContentHeight(unbounded) is what pins the tall child to the top: a plain
                // height/requiredHeight that overflows its parent gets *centred*, which silently
                // shifts every offset by half the overflow.
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .height(fullHeightDp.dp)
                .offset(y = -offsetDp.dp),
        ) {
            content()
        }
    }
}

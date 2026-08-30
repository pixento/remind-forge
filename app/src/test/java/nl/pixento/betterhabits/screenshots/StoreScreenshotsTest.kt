package nl.pixento.betterhabits.screenshots

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import nl.pixento.betterhabits.domain.model.IntervalRandomness
import nl.pixento.betterhabits.domain.model.VibrationPatternType
import nl.pixento.betterhabits.ui.settings.SettingsScreen
import nl.pixento.betterhabits.ui.settings.SettingsUiState
import nl.pixento.betterhabits.ui.settings.vibration.VibrationPatternScreen
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the phone screenshots for the Play listing, in every language the app ships.
 *
 * Not a test in the usual sense - it asserts only that each image landed at the size Play accepts;
 * what the images *look* like is judged by eye. It lives in the unit test source set because
 * Roborazzi draws Compose on the JVM under Robolectric, and only writes files while
 * `roborazzi.test.record` is set, which `app/build.gradle.kts` ties to
 * `-PrecordStoreGraphics=true`. A plain `testDebugUnitTest` skips this class entirely.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The canvas size comes from the device qualifier and nothing else: Robolectric derives the pixel
// size as dp * density, and xxhdpi is density 3, so this is an exact 1080x1920 - a 9:16 phone
// screenshot inside Play's 320..3840px per side.
@Config(qualifiers = "w360dp-h640dp-port-notnight-xxhdpi")
class StoreScreenshotsTest {

    private class Shot(val name: String, val content: @Composable () -> Unit)

    // Pinned, because the screen formats this through ZoneId.systemDefault() - otherwise a laptop
    // and the CI runner would render different times into "Next reminder around ...".
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val nextTrigger =
        LocalDateTime.of(2026, 1, 5, 14, 20).atZone(zone).toInstant().toEpochMilli()

    private val shots = listOf(
        Shot("reminders") {
            AppScreen { contentPadding ->
                settings(
                    contentPadding = contentPadding,
                    uiState = SettingsUiState(
                        enabled = true,
                        intervalMinutes = 20,
                        nextTriggerAtMillis = nextTrigger,
                        vibrationPattern = VibrationPatternType.DOUBLE_PULSE,
                        ringtoneUri = RINGTONE_URI,
                        ringtoneTitle = RINGTONE_TITLE,
                    ),
                )
            }
        },
        Shot("interval") {
            AppScreen { contentPadding ->
                // Frames the whole Schedule section, from its header to the Do Not Disturb row.
                ScrolledTo(offsetDp = 212) {
                    settings(
                        contentPadding = contentPadding,
                        uiState = SettingsUiState(
                            enabled = true,
                            intervalMinutes = 45,
                            intervalRandomness = IntervalRandomness.TWENTY_PERCENT,
                            windowStart = LocalTime.of(7, 30),
                            windowEnd = LocalTime.of(22, 0),
                            nextTriggerAtMillis = nextTrigger,
                        ),
                    )
                }
            }
        },
        Shot("do_not_disturb") {
            AppScreen { contentPadding ->
                // Far enough down to put the whole Automatic pauses card in frame with both
                // conditions ticked - that card, not the Schedule above it, is what this shot is
                // captioned for. One offset serves every locale, so it's set by the longest
                // translation rather than by English: at 460 the German and French rows wrap to
                // three lines and push "pause while Android Auto is connected" off the bottom,
                // leaving a caption that promises a row the image doesn't show.
                ScrolledTo(offsetDp = 560) {
                    settings(
                        contentPadding = contentPadding,
                        uiState = SettingsUiState(
                            enabled = true,
                            pauseDuringDoNotDisturb = true,
                            pauseDuringAndroidAuto = true,
                            doNotDisturbActive = true,
                            nextTriggerAtMillis = nextTrigger,
                        ),
                    )
                }
            }
        },
        Shot("alerts") {
            AppScreen { contentPadding ->
                // The Alerts card is the last thing on the screen, so this is near the bottom.
                ScrolledTo(offsetDp = 505) {
                    settings(
                        contentPadding = contentPadding,
                        uiState = SettingsUiState(
                            enabled = true,
                            nextTriggerAtMillis = nextTrigger,
                            vibrationPattern = VibrationPatternType.LONG_SHORT_SHORT,
                            ringtoneUri = RINGTONE_URI,
                            ringtoneTitle = RINGTONE_TITLE,
                        ),
                    )
                }
            }
        },
        Shot("vibration") {
            AppWindow {
                VibrationPatternScreen(
                    selected = VibrationPatternType.LONG_SHORT_SHORT,
                    onSelect = {},
                    onBack = {},
                )
            }
        },
    )

    /** The same call shape as `SettingsScreenTest.setScreen` - every callback stubbed. */
    @Composable
    private fun settings(contentPadding: PaddingValues, uiState: SettingsUiState) = SettingsScreen(
        uiState = uiState,
        onEnabledChanged = {},
        onIntervalChanged = { _, _ -> },
        onLimitToActiveHoursChanged = {},
        onWindowStartChanged = {},
        onWindowEndChanged = {},
        onVibrationPatternSelected = {},
        onRingtoneSelected = {},
        onRequestExactAlarmPermission = {},
        contentPadding = contentPadding,
    )

    private val originalLocale = Locale.getDefault()
    private val originalZone = TimeZone.getDefault()

    @Before
    fun pinEnvironment() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    @After
    fun restoreEnvironment() {
        TimeZone.setDefault(originalZone)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun renderPhoneScreenshots() {
        shippedLocales.forEach { locale ->
            val captions = storeCopy("captions", locale, expectedLines = shots.size)
            // A leading '+' modifies the current configuration instead of replacing it, so the
            // size and density qualifiers above survive. It touches only Android's Configuration,
            // never the JVM default locale - hence the setDefault next to it.
            RuntimeEnvironment.setQualifiers("+${locale.res}")
            Locale.setDefault(locale.locale)

            shots.forEachIndexed { index, shot ->
                val file = storeImage(
                    locale = locale,
                    relativePath = "phoneScreenshots/%02d_%s.png".format(index + 1, shot.name),
                )
                captureRoboImage(filePath = file.absolutePath) {
                    StoreScreenshotFrame(caption = captions[index]) { shot.content() }
                }
                assertRendered(file, width = 1080, height = 1920)
            }
        }
    }

    private companion object {
        const val RINGTONE_URI = "content://settings/system/notification_sound"
        const val RINGTONE_TITLE = "Chime"
    }
}

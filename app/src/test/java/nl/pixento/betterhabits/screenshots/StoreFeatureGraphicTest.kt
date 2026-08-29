package nl.pixento.betterhabits.screenshots

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import nl.pixento.betterhabits.R
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the Play feature graphic - the banner at the top of the listing - in every language.
 *
 * Its own class purely so it can carry its own device qualifier: `@Config(qualifiers = ...)` is per
 * class, and this one is a completely different shape from the phone screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// 512dp x 250dp at xhdpi (density 2) is exactly the 1024x500 Play requires, to the pixel.
@Config(qualifiers = "w512dp-h250dp-land-notnight-xhdpi")
class StoreFeatureGraphicTest {

    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun renderFeatureGraphic() {
        shippedLocales.forEach { locale ->
            val tagline = storeCopy("feature-graphic", locale, expectedLines = 1).single()
            RuntimeEnvironment.setQualifiers("+${locale.res}")
            Locale.setDefault(locale.locale)

            // The name is the same in every locale - it's a proper noun, not something translated -
            // but read it from resources anyway so the banner can never drift from the app.
            val appName = ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.app_name)

            val file = storeImage(locale, relativePath = "featureGraphic.png")
            captureRoboImage(filePath = file.absolutePath) {
                StoreFeatureGraphic(appName = appName, tagline = tagline)
            }
            assertRendered(file, width = 1024, height = 500)
        }
    }
}

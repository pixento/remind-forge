package nl.pixento.betterhabits.screenshots

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the 512x512 Play icon from the same vector drawables the launcher icon is built from,
 * rather than upscaling the 192px `mipmap-xxxhdpi` PNG.
 *
 * No locale loop: the icon carries no text. It is still written into every locale directory, since
 * fastlane's layout has no locale-independent slot for it and a uniform tree can be handed to
 * `supply` for any locale without a special case.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// mdpi is density 1, so 512dp is 512px on the nose.
@Config(qualifiers = "w512dp-h512dp-port-notnight-mdpi")
class StoreAppIconTest {

    @Test
    fun renderAppIcon() {
        shippedLocales.forEach { locale ->
            val file = storeImage(locale, relativePath = "icon.png")
            captureRoboImage(filePath = file.absolutePath) { StoreAppIcon() }
            assertRendered(file, width = 512, height = 512)
        }
    }
}

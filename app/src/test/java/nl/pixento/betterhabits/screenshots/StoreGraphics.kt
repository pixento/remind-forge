package nl.pixento.betterhabits.screenshots

import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

/**
 * Shared plumbing for the Play listing graphics rendered out of `src/test` - see
 * [StoreScreenshotsTest], [StoreFeatureGraphicTest] and [StoreAppIconTest].
 *
 * Nothing here is a test. These renderers live in the unit test source set because Roborazzi draws
 * Compose on the JVM under Robolectric, which is the only place that machinery exists - not because
 * the images assert anything about the app.
 */

/**
 * A language the app ships, paired with the Play listing locale that names its output directory.
 *
 * The two differ on purpose: `res/values-*` is qualified by language only, while Play (and
 * `distribution/whatsnew`, and `store-listing.md`) uses region-qualified locales. [res] drives
 * Robolectric's qualifiers, [play] is only ever a directory or file name.
 */
internal data class ShippedLocale(val play: String, val res: String) {
    val locale: Locale get() = Locale.forLanguageTag(play)
}

internal val shippedLocales = listOf(
    ShippedLocale(play = "en-GB", res = "en"),
    ShippedLocale(play = "nl-NL", res = "nl"),
    ShippedLocale(play = "de-DE", res = "de"),
    ShippedLocale(play = "es-ES", res = "es"),
    ShippedLocale(play = "fr-FR", res = "fr"),
)

/** Absolute paths handed down by `app/build.gradle.kts`, so nothing depends on the test JVM's cwd. */
private fun requiredPath(property: String): File =
    File(
        requireNotNull(System.getProperty(property)) {
            "$property is unset. Render the store graphics with " +
                "./gradlew :app:testDebugUnitTest -PrecordStoreGraphics=true"
        },
    )

private val outputRoot: File get() = requiredPath("storeGraphics.outputDir")
private val copyRoot: File get() = requiredPath("storeGraphics.copyDir")

/**
 * Where an image goes, in fastlane supply's layout, so the whole tree can be handed to `supply` or
 * uploaded through the Play Console without rearranging anything.
 */
internal fun storeImage(locale: ShippedLocale, relativePath: String): File =
    File(outputRoot, "metadata/android/${locale.play}/images/$relativePath")

/**
 * Store copy for one locale, from `distribution/screenshots/<set>/<play-locale>`.
 *
 * Blank lines and `#` comments are ignored, so the files can carry a note about what they feed.
 * Lint enforces nothing out here - the [expectedLines] check is what stops a locale falling behind,
 * the same way `MissingTranslation` does for the app's own string resources.
 */
internal fun storeCopy(set: String, locale: ShippedLocale, expectedLines: Int): List<String> {
    val file = File(copyRoot, "$set/${locale.play}")
    check(file.isFile) { "Missing store copy: ${file.absolutePath}" }
    val lines = file.readLines()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }
    check(lines.size == expectedLines) {
        "${file.absolutePath} has ${lines.size} lines, expected $expectedLines"
    }
    return lines
}

/**
 * Fails if a capture didn't land, came out the wrong size, or carries an alpha channel.
 *
 * The size check is the one that matters most: it catches the silent no-op where
 * `roborazzi.test.record` is false and `captureRoboImage` returns without writing anything, and it
 * catches a device qualifier that didn't take. Play rejects transparency in the feature graphic and
 * the icon, and the screenshots have no reason to carry it either.
 */
internal fun assertRendered(file: File, width: Int, height: Int) {
    check(file.isFile && file.length() > 0) { "Nothing was written to ${file.absolutePath}" }
    val image = requireNotNull(ImageIO.read(file)) { "Not a readable PNG: ${file.absolutePath}" }
    check(image.width == width && image.height == height) {
        "${file.name} is ${image.width}x${image.height}, expected ${width}x$height"
    }
    listOf(0 to 0, width - 1 to 0, 0 to height - 1, width - 1 to height - 1, width / 2 to height / 2)
        .forEach { (x, y) ->
            val alpha = image.getRGB(x, y) ushr 24
            check(alpha == 0xFF) { "${file.name} is transparent at ($x, $y)" }
        }
}

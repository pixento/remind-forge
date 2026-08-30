package nl.pixento.betterhabits.distribution

import nl.pixento.betterhabits.screenshots.shippedLocales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards `distribution/whatsnew`, the Play release notes the release workflow uploads verbatim.
 *
 * The notes are generated rather than hand-written (see `.claude/skills/whatsnew`), which is exactly
 * why they need a mechanical check: nothing else looks at them until a `v*` tag is pushed, and by
 * then a missing locale or an over-long file fails the Play upload at the worst possible moment.
 * This is to `distribution/whatsnew` what lint's `MissingTranslation` is to `res/values-*`.
 *
 * Plain JUnit - no Robolectric, no Compose - and deliberately outside the `screenshots` package,
 * which `app/build.gradle.kts` filters out of an ordinary `testDebugUnitTest` run.
 */
class WhatsNewFilesTest {

    /** Play's limit on a release notes entry. Anything longer is rejected on upload. */
    private val maxLength = 500

    private val whatsNewDir: File =
        File(
            requireNotNull(System.getProperty("whatsNew.dir")) {
                "whatsNew.dir is unset; app/build.gradle.kts hands it to every Test task"
            },
        )

    @Test
    fun `every shipped locale has release notes`() {
        shippedLocales.forEach { locale ->
            val file = File(whatsNewDir, "whatsnew-${locale.play}")
            assertTrue("Missing release notes: ${file.absolutePath}", file.isFile)
            assertTrue(
                "Empty release notes: ${file.absolutePath}",
                file.readText().isNotBlank(),
            )
        }
    }

    /** A locale that is missing entirely is the test above's failure to report, not this one's. */
    @Test
    fun `release notes fit Play's length limit`() {
        shippedLocales.forEach { locale ->
            val file = File(whatsNewDir, "whatsnew-${locale.play}").takeIf { it.isFile } ?: return@forEach
            val length = file.readText().trim().length
            assertTrue(
                "${file.name} is $length characters, Play allows at most $maxLength",
                length <= maxLength,
            )
        }
    }

    /**
     * The whole directory is handed to the Play upload as `whatsNewDirectory`, so a stray file next
     * to the notes is a file that gets uploaded - or, for a locale the listing doesn't have, an
     * upload that fails.
     */
    @Test
    fun `the directory holds nothing but the shipped locales`() {
        val expected = shippedLocales.map { "whatsnew-${it.play}" }.toSet()
        val stray = (whatsNewDir.listFiles() ?: emptyArray()).map { it.name } - expected
        assertEquals("Unexpected contents in ${whatsNewDir.absolutePath}", emptyList<String>(), stray)
    }
}

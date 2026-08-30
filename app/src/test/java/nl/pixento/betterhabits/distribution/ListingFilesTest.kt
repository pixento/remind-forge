package nl.pixento.betterhabits.distribution

import nl.pixento.betterhabits.screenshots.shippedLocales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards `distribution/listings`, the Play listing copy the promote workflow uploads verbatim.
 *
 * Sibling of [WhatsNewFilesTest], and there for the same reason: nothing reads these files until a
 * production promotion is under way, and by then an over-long description or a missing locale fails
 * the upload halfway through an edit that also carries the release. The character limits are Play's
 * own, enforced per locale.
 */
class ListingFilesTest {

    /** Play's per-locale limits on the three listing fields. */
    private val limits = mapOf(
        "title.txt" to 30,
        "short_description.txt" to 80,
        "full_description.txt" to 4000,
    )

    private val listingsDir: File =
        File(
            requireNotNull(System.getProperty("listings.dir")) {
                "listings.dir is unset; app/build.gradle.kts hands it to every Test task"
            },
        )

    @Test
    fun `every shipped locale has all three listing fields`() {
        shippedLocales.forEach { locale ->
            limits.keys.forEach { name ->
                val file = File(listingsDir, "${locale.play}/$name")
                assertTrue("Missing listing copy: ${file.absolutePath}", file.isFile)
                assertTrue("Empty listing copy: ${file.absolutePath}", file.readText().isNotBlank())
            }
        }
    }

    /** A file that is missing entirely is the test above's failure to report, not this one's. */
    @Test
    fun `listing copy fits Play's length limits`() {
        shippedLocales.forEach { locale ->
            limits.forEach { (name, limit) ->
                val file = File(listingsDir, "${locale.play}/$name").takeIf { it.isFile }
                    ?: return@forEach
                val length = file.readText().trim().length
                assertTrue(
                    "${locale.play}/$name is $length characters, Play allows at most $limit",
                    length <= limit,
                )
            }
        }
    }

    /**
     * The title and short description reach Play as single-line fields, so a stray line break in
     * either is silently lost rather than rejected.
     */
    @Test
    fun `the title and short description are single lines`() {
        shippedLocales.forEach { locale ->
            listOf("title.txt", "short_description.txt").forEach { name ->
                val file = File(listingsDir, "${locale.play}/$name").takeIf { it.isFile }
                    ?: return@forEach
                assertEquals(
                    "${locale.play}/$name spans more than one line",
                    1,
                    file.readText().trim().lines().size,
                )
            }
        }
    }

    /**
     * The uploader walks this tree rather than being handed a file list, so anything unexpected in
     * it is either published or an upload that fails - the same hazard `whatsnew/` has.
     */
    @Test
    fun `the directory holds nothing but the shipped locales and their three fields`() {
        val expectedLocales = shippedLocales.map { it.play }.toSet()
        val strayLocales = (listingsDir.listFiles() ?: emptyArray()).map { it.name } - expectedLocales
        assertEquals(
            "Unexpected contents in ${listingsDir.absolutePath}",
            emptyList<String>(),
            strayLocales,
        )

        shippedLocales.forEach { locale ->
            val dir = File(listingsDir, locale.play)
            val stray = (dir.listFiles() ?: emptyArray()).map { it.name } - limits.keys
            assertEquals("Unexpected contents in ${dir.absolutePath}", emptyList<String>(), stray)
        }
    }
}

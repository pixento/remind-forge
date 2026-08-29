package nl.pixento.betterhabits.domain.model

import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyWindowTest {

    @Test
    fun `daytime window includes its start and excludes its end`() {
        val window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertTrue(window.contains(LocalTime.of(9, 0)))
        assertTrue(window.contains(LocalTime.of(12, 30)))
        assertFalse(window.contains(LocalTime.of(17, 0)))
        assertFalse(window.contains(LocalTime.of(8, 59)))
        assertFalse(window.contains(LocalTime.of(20, 0)))
    }

    @Test
    fun `overnight window wraps past midnight`() {
        val window = DailyWindow(LocalTime.of(22, 0), LocalTime.of(6, 0))

        assertTrue(window.contains(LocalTime.of(22, 0)))
        assertTrue(window.contains(LocalTime.of(23, 30)))
        assertTrue(window.contains(LocalTime.MIDNIGHT))
        assertTrue(window.contains(LocalTime.of(5, 59)))
        assertFalse(window.contains(LocalTime.of(6, 0)))
        assertFalse(window.contains(LocalTime.NOON))
    }

    @Test
    fun `equal start and end means always active`() {
        val window = DailyWindow(LocalTime.of(9, 0), LocalTime.of(9, 0))

        assertTrue(window.contains(LocalTime.of(9, 0)))
        assertTrue(window.contains(LocalTime.of(3, 0)))
        assertTrue(window.contains(LocalTime.of(21, 0)))
    }
}

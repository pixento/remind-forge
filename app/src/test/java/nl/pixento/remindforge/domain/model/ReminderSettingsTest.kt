package nl.pixento.remindforge.domain.model

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ReminderSettings.schedulesSameAs] decides whether a settings write restarts the alarm chain. Get
 * it wrong in one direction and an interval change waits out the in-flight chain; wrong in the other
 * and merely picking a ringtone pushes the next reminder a full interval away.
 */
class ReminderSettingsTest {

    private val settings = ReminderSettings(
        enabled = true,
        intervalMinutes = 15,
        windowStart = LocalTime.of(9, 0),
        windowEnd = LocalTime.of(17, 0),
    )

    @Test
    fun `identical settings schedule the same`() {
        assertTrue(settings.schedulesSameAs(settings.copy()))
    }

    @Test
    fun `an alert channel change leaves the schedule untouched`() {
        // The whole point: every tick re-reads settings, so the running chain already picks these
        // up and restarting it would only delay the next reminder.
        assertTrue(
            settings.schedulesSameAs(
                settings.copy(vibrationPattern = VibrationPatternType.TRIPLE_PULSE),
            )
        )
        assertTrue(
            settings.schedulesSameAs(settings.copy(ringtoneUri = "content://media/ringtone/42"))
        )
        assertTrue(
            settings.copy(ringtoneUri = "content://media/ringtone/42").schedulesSameAs(
                settings.copy(ringtoneUri = null),
            )
        )
    }

    @Test
    fun `each input the chain is computed from forces a reschedule`() {
        assertFalse(settings.schedulesSameAs(settings.copy(enabled = false)))
        assertFalse(settings.schedulesSameAs(settings.copy(intervalMinutes = 20)))
        assertFalse(
            settings.schedulesSameAs(
                settings.copy(intervalRandomness = IntervalRandomness.TWENTY_PERCENT),
            )
        )
        assertFalse(settings.schedulesSameAs(settings.copy(windowStart = LocalTime.of(8, 0))))
        assertFalse(settings.schedulesSameAs(settings.copy(windowEnd = LocalTime.of(18, 0))))
        assertFalse(
            settings.schedulesSameAs(
                settings.copy(activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF),
            )
        )
    }

    @Test
    fun `custom times exposes the stored window, following Do Not Disturb exposes none`() {
        // A null window is what tells NextTriggerCalculator not to clamp: whether DND is on can
        // only be read for now, never predicted for the instant the next tick will land on.
        assertEquals(DailyWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)), settings.activeWindow)
        assertNull(settings.copy(activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF).activeWindow)
    }
}

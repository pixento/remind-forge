package nl.pixento.betterhabits.ui.settings.components

import android.media.RingtoneManager
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RingtonePickerLauncherTest {

    @Test
    fun `picker intent requests alarm-type sounds so hardware volume keys control STREAM_ALARM`() {
        val intent = buildRingtonePickerIntent(currentUri = null)

        assertEquals(RingtoneManager.ACTION_RINGTONE_PICKER, intent.action)
        assertEquals(
            RingtoneManager.TYPE_ALARM,
            intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1),
        )
        assertTrue(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false))
    }

    @Test
    fun `picker intent offers Silent so the sound channel can be switched off`() {
        val intent = buildRingtonePickerIntent(currentUri = null)

        assertTrue(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false))
    }

    @Test
    fun `null current uri omits the existing-uri extra`() {
        val intent = buildRingtonePickerIntent(currentUri = null)

        assertFalse(intent.hasExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `non-null current uri is passed through as the existing-uri extra`() {
        val uri = Uri.parse("content://media/external/audio/media/42")

        val intent = buildRingtonePickerIntent(currentUri = uri)

        assertEquals(
            uri,
            intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI),
        )
    }
}

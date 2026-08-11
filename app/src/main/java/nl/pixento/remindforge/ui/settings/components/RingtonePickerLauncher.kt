package nl.pixento.remindforge.ui.settings.components

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri

fun buildRingtonePickerIntent(currentUri: Uri?): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        // "Silent" is how the sound channel gets switched off; the picker returns a null URI for it.
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        if (currentUri != null) {
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
        }
    }

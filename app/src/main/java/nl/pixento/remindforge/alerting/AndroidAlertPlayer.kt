package nl.pixento.remindforge.alerting

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import nl.pixento.remindforge.domain.model.VibrationPatternType

class AndroidAlertPlayer(
    private val context: Context,
) : AlertPlayer {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentRingtone: Ringtone? = null
    private var pendingStop: Runnable? = null

    /**
     * Alarm-classed so the OS treats vibration/ringtone playback like an alarm clock rather than
     * a notification: it isn't silenced by ringer mode (silent/vibrate) or a Do Not Disturb
     * policy the way default-classed playback would be - both of which real devices otherwise
     * apply to unattributed Vibrator/Ringtone calls.
     */
    private val alarmAudioAttributes: AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun playVibration(pattern: VibrationPatternType) {
        // Guarding here rather than at each call site covers both the alarm chain and the
        // settings-screen preview in one place.
        val waveform = VibrationPatterns.waveformFor(pattern) ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val effect = VibrationEffect.createWaveform(waveform, -1)
                val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
                vibrator.vibrate(effect, attributes)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createWaveform(waveform, -1), alarmAudioAttributes)
            }

            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(waveform, -1, alarmAudioAttributes)
            }
        }
    }

    override fun playRingtone(uriString: String) {
        stopCurrentRingtone()
        val uri = Uri.parse(uriString)
        val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
        ringtone.audioAttributes = alarmAudioAttributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.isLooping = false
        }
        currentRingtone = ringtone
        ringtone.play()

        // USAGE_ALARM playback rings until dismissed on real devices - explicit setLooping(false)
        // above isn't honored on every OS version - so this is stopped after one playthrough of
        // the sound's own length rather than left to loop indefinitely; a periodic reminder
        // should buzz once, not ring like an alarm clock until the user acts. The fixed fallback
        // only applies if the sound's duration couldn't be read.
        val stop = Runnable {
            if (currentRingtone === ringtone) stopCurrentRingtone()
        }
        pendingStop = stop
        mainHandler.postDelayed(stop, ringtoneDurationMillis(uri) ?: FALLBACK_DURATION_MILLIS)
    }

    /** Idempotent: safe when nothing is playing, and cancels any queued auto-stop. */
    private fun stopCurrentRingtone() {
        pendingStop?.let { mainHandler.removeCallbacks(it) }
        pendingStop = null
        currentRingtone?.let { ringtone -> if (ringtone.isPlaying) ringtone.stop() }
        currentRingtone = null
    }

    private fun ringtoneDurationMillis(uri: Uri): Long? {
        // Picking "Default" in the picker stores the settings-alias URI rather than the concrete
        // sound file; RingtoneManager.getRingtone() resolves that alias internally for playback,
        // but MediaMetadataRetriever can't open it directly, so it must be resolved here too.
        val resolvedUri = if (uri == Settings.System.DEFAULT_ALARM_ALERT_URI) {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM) ?: uri
        } else {
            uri
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, resolvedUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private companion object {
        private const val FALLBACK_DURATION_MILLIS = 5_000L
    }
}

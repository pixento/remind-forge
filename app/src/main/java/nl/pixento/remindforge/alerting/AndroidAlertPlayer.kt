package nl.pixento.remindforge.alerting

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import nl.pixento.remindforge.domain.model.VibrationPatternType

class AndroidAlertPlayer(
    private val context: Context,
) : AlertPlayer {

    private var currentRingtone: Ringtone? = null

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
        val waveform = VibrationPatterns.waveformFor(pattern)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                vibrator.vibrate(VibrationEffect.createWaveform(waveform, -1))

            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(waveform, -1)
            }
        }
    }

    override fun playRingtone(uriString: String) {
        stopPreview()
        val uri = Uri.parse(uriString)
        val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
        currentRingtone = ringtone
        ringtone.play()
    }

    override fun stopPreview() {
        currentRingtone?.let { ringtone -> if (ringtone.isPlaying) ringtone.stop() }
        currentRingtone = null
    }
}

package nl.pixento.remindforge.alerting

import nl.pixento.remindforge.domain.model.VibrationPatternType

/**
 * Plays alerts directly (bypassing notification-channel sound/vibration) so the user's chosen
 * pattern/ringtone can change at any time without recreating a channel. Also used by the
 * settings-screen preview button, so preview and real alerts share one code path.
 *
 * [playRingtone] takes a plain URI string (rather than [android.net.Uri]) so this interface -
 * and the domain-layer code that calls it - stays free of Android framework types.
 */
interface AlertPlayer {
    fun playVibration(pattern: VibrationPatternType)
    fun playRingtone(uriString: String)
    fun stopPreview()
}

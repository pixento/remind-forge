package nl.pixento.betterhabits.alerting

import nl.pixento.betterhabits.domain.model.VibrationPatternType

/**
 * Plays alerts directly (bypassing notification-channel sound/vibration) so the user's chosen
 * pattern/ringtone can change at any time without recreating a channel. Also used by the vibration
 * picker to audition a pattern on tap, so previews and real alerts share one code path.
 *
 * [playRingtone] takes a plain URI string (rather than [android.net.Uri]) so this interface -
 * and the domain-layer code that calls it - stays free of Android framework types.
 */
interface AlertPlayer {
    fun playVibration(pattern: VibrationPatternType)
    fun playRingtone(uriString: String)
}

package nl.pixento.remindforge.alerting

import android.content.Context
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import nl.pixento.remindforge.domain.model.VibrationPatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidAlertPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val player = AndroidAlertPlayer(context)

    @Test
    fun `playVibration triggers an alarm-classed vibration`() {
        player.playVibration(VibrationPatternType.SHORT_PULSE)

        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator: Vibrator = vibratorManager.defaultVibrator
        val shadowVibrator = shadowOf(vibrator)
        assertTrue(shadowVibrator.isVibrating)

        val attributes = shadowVibrator.vibrationAttributesFromLastVibration as VibrationAttributes
        assertEquals(VibrationAttributes.USAGE_ALARM, attributes.usage)
    }

    @Test
    fun `alarm audio attributes are classed as USAGE_ALARM`() {
        // Same AudioAttributes instance backs playRingtone; RingtoneManager.getRingtone can't be
        // exercised end-to-end under Robolectric (no resolvable media Uri on the JVM), so this
        // asserts the shared attributes object directly via reflection.
        val field = AndroidAlertPlayer::class.java.getDeclaredField("alarmAudioAttributes")
        field.isAccessible = true
        val attributes = field.get(player) as android.media.AudioAttributes
        assertEquals(android.media.AudioAttributes.USAGE_ALARM, attributes.usage)
    }

    @Test
    fun `playRingtone with an unresolvable uri does not throw`() {
        player.playRingtone("content://media/external/audio/media/999999")
    }

    @Test
    fun `playRingtone with the default-alarm alias uri does not throw`() {
        // Picking "Default" in the picker stores this settings-alias uri rather than a concrete
        // sound file; it must be resolved before duration lookup instead of being passed as-is.
        player.playRingtone(Settings.System.DEFAULT_ALARM_ALERT_URI.toString())
    }
}

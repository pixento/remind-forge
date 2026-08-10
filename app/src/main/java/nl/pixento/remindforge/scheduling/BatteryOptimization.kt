package nl.pixento.remindforge.scheduling

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Optional, user-opt-in exemption: some OEMs (Xiaomi/Huawei/Samsung, ...) aggressively kill
 * scheduled alarms to save battery even when SCHEDULE_EXACT_ALARM is granted. This is a UX
 * nudge, not something requested silently on the user's behalf.
 */
object BatteryOptimization {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun buildRequestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
}

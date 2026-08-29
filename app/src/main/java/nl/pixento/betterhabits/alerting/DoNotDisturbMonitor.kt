package nl.pixento.betterhabits.alerting

/**
 * Reads whether the phone is currently in Do Not Disturb. Split from its Android implementation the
 * same way [AlertPlayer] is, so the alarm-chain use case stays a plain JVM unit test.
 */
interface DoNotDisturbMonitor {
    fun isDoNotDisturbActive(): Boolean
}

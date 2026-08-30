package nl.pixento.betterhabits.alerting

/**
 * Reads whether the phone is currently connected to a car. Split from its Android implementation the
 * same way [DoNotDisturbMonitor] is, so the alarm-chain use case stays a plain JVM unit test.
 */
interface CarConnectionMonitor {
    fun isConnectedToCar(): Boolean
}

package nl.pixento.betterhabits.scheduling

interface AlarmScheduler {
    fun scheduleNext(triggerAtMillis: Long)
    fun cancel()
    fun hasPending(): Boolean
}

package nl.pixento.remindforge.scheduling

interface AlarmScheduler {
    fun scheduleNext(triggerAtMillis: Long)
    fun cancel()
}

package com.kyant.backdrop.internal

/**
 * One scheduled trailing event, with immediate first delivery and no idle
 * work.
 */
internal class EventThrottle<T : Any>(
    private val intervalMillis: Long,
    private val nowMillis: () -> Long,
    private val schedule: (Long, () -> Unit) -> Unit,
    private val cancelScheduled: () -> Unit,
    private val emit: (T) -> Unit,
) {
    private var pending: T? = null
    private var scheduled = false
    private var lastEmission: Long? = null
    private var generation = 0

    fun offer(value: T) {
        pending = value
        if (scheduled) return
        scheduled = true
        val token = generation
        val delay = lastEmission?.let { (intervalMillis - (nowMillis() - it)).coerceAtLeast(0) } ?: 0
        schedule(delay) {
            if (token == generation) {
                scheduled = false
                val latest = pending
                pending = null
                if (latest != null) {
                    lastEmission = nowMillis()
                    emit(latest)
                }
            }
        }
    }

    fun cancel() {
        generation++
        cancelScheduled()
        scheduled = false
        pending = null
        lastEmission = null
    }
}

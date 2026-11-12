package com.kyant.backdrop

import com.kyant.backdrop.internal.EventThrottle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventThrottleTest {
    private class Fixture(interval: Long = 200) {
        var time = 0L
        var task: (() -> Unit)? = null
        var due = 0L
        val output = mutableListOf<Pair<Long, Int>>()
        val throttle = EventThrottle<Int>(
            interval, { time },
            { delay, callback -> due = time + delay; task = callback },
            { task = null }, { output += time to it })

        fun advance(to: Long) {
            while (task != null && due <= to) {
                time = due
                val callback = task!!
                task = null
                callback()
            }
            time = to
        }
    }

    @Test
    fun continuousEventsEmitEveryIntervalWithLatestValue() {
        val f = Fixture()
        f.throttle.offer(0)
        f.advance(0)
        for (i in 1..9) {
            f.advance(i * 50L); f.throttle.offer(i)
        }
        f.advance(600)
        assertEquals(listOf(0L to 0, 200L to 3, 400L to 7, 600L to 9), f.output)
        assertNull(f.task) // No polling or recurring idle task.
    }

    @Test
    fun cancellationDiscardsEvenAlreadyQueuedCallback() {
        val f = Fixture()
        f.throttle.offer(1)
        val stale = f.task!!
        f.throttle.cancel()
        f.throttle.offer(2)
        stale()
        f.advance(0)
        assertEquals(listOf(0L to 2), f.output)
    }

    @Test
    fun zeroIntervalStillCoalescesQueuedEvents() {
        val f = Fixture(0)
        f.throttle.offer(1)
        f.throttle.offer(2)
        f.advance(0)
        f.advance(1)
        f.throttle.offer(3)
        f.advance(1)
        assertEquals(listOf(0L to 2, 1L to 3), f.output)
        assertNull(f.task)
    }
}

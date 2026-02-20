package io.bluetape4k.io.okio

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class TimeoutSupportTest {

    @Test
    fun `infiniteTimeout sets zero timeout`() {
        val timeout = infiniteTimeout()
        timeout.timeoutNanos() shouldBeEqualTo 0L
    }

    @Test
    fun `duration toTimeout and toDeadline`() {
        val duration = Duration.ofMillis(1500)

        val timeout = duration.toTimeout()
        timeout.timeoutNanos() shouldBeEqualTo duration.toNanos()

        val deadline = duration.toDeadline()
        val remaining = deadline.deadlineNanoTime() - System.nanoTime()
        val tolerance = TimeUnit.MILLISECONDS.toNanos(5)
        val withinTolerance = kotlin.math.abs(remaining - duration.toNanos()) <= tolerance
        withinTolerance shouldBeEqualTo true
    }
}

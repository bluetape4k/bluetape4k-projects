package io.bluetape4k.micrometer.instrument

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TimerExtensionsTest {

    private val registry = SimpleMeterRegistry()

    @Test
    fun `recordSuspend should measure suspend block and return value`() = runSuspendIO {
        val timer = registry.timer("infra.micrometer.timer.recordSuspend")
        val result = timer.recordSuspend {
            delay(1)
            "completed"
        }

        result shouldBeEqualTo "completed"
        timer.count() shouldBeEqualTo 1L
        timer.totalTime(TimeUnit.NANOSECONDS) shouldBeGreaterThan 0.0
    }

    @Test
    fun `withTimer should record flow lifecycle once`() = runSuspendIO {
        val timer = registry.timer("infra.micrometer.timer.flow")
        flowOf(1, 2, 3).withTimer(timer).collect()

        timer.count() shouldBeEqualTo 1L
        timer.totalTime(TimeUnit.NANOSECONDS) shouldBeGreaterThan 0.0
    }
}

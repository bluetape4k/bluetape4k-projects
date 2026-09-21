package io.bluetape4k.micrometer.instrument

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.micrometer.AbstractMicrometerTest
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class TimerExtensionsTest: AbstractMicrometerTest() {

    companion object: KLogging()

    private val registry = SimpleMeterRegistry()

    @Test
    fun `recordSuspend should measure suspend block and return value`() = runSuspendIO {
        val timer = registry.timer("infra.micrometer.timer.recordSuspend")
        val result = timer.recordSuspend {
            delay(1.milliseconds)
            "completed"
        }
        result shouldBeEqualTo "completed"

        log.debug { timer.describe() }
        timer.count() shouldBeEqualTo 1L
        timer.totalTime(TimeUnit.NANOSECONDS) shouldBeGreaterThan 0.0
    }

    @Test
    fun `recordSuspend should record failed block`() = runSuspendIO {
        val timer = registry.timer("infra.micrometer.timer.recordSuspend.failure")

        assertFailsWith<IllegalStateException> {
            timer.recordSuspend {
                delay(1.milliseconds)
                throw IllegalStateException("boom")
            }
        }

        log.debug { timer.describe() }
        timer.count() shouldBeEqualTo 1L
    }

    @Test
    fun `withTimer should record flow lifecycle once`() = runSuspendIO {
        val timer = registry.timer("infra.micrometer.timer.flow")

        flowOf(1, 2, 3).withTimer(timer).collect()

        log.debug { timer.describe() }
        timer.count() shouldBeEqualTo 1L
        timer.totalTime(TimeUnit.NANOSECONDS) shouldBeGreaterThan 0.0
    }

    @Test
    fun `withTimer should record each collection independently`() = runSuspendIO {
        val timer: Timer = registry.timer("infra.micrometer.timer.multi.collect")
        val measuredFlow = flow {
            emit(1)
            delay(1.milliseconds)
            emit(2)
        }.withTimer(timer)

        measuredFlow.collect()
        log.debug { timer.describe() }

        measuredFlow.collect()
        log.debug { timer.describe() }

        timer.count() shouldBeEqualTo 2L
    }

    @Test
    fun `withTimer should record failed collection`() = runSuspendIO {
        val timer = registry.timer("infra.micrometer.timer.failure")

        assertFailsWith<IllegalStateException> {
            flow {
                emit(1)
                throw IllegalStateException("boom")
            }.withTimer(timer).collect()
        }

        log.debug { timer.describe() }
        
        timer.count() shouldBeEqualTo 1L
        timer.totalTime(TimeUnit.NANOSECONDS) shouldBeGreaterThan 0.0
    }

    private fun Timer.describe(): String =
        "timer count=${count()}, total time=${totalTime(TimeUnit.NANOSECONDS)} nanos"

}

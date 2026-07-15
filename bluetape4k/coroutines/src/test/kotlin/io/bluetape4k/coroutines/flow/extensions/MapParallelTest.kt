package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MapParallelTest {

    @Test
    fun `parallelism 1 maintains order`() = runTest {
        val result = (1..5).asFlow()
            .mapParallel(parallelism = 1) { it * 2 }
            .toList()

        result shouldBeEqualTo listOf(2, 4, 6, 8, 10)
    }

    @Test
    fun `parallelism 0 coerces to sequential mapping`() = runTest {
        val result = (1..5).asFlow()
            .mapParallel(parallelism = 0) { it * 3 }
            .toList()

        result shouldBeEqualTo listOf(3, 6, 9, 12, 15)
    }

    @Test
    fun `negative parallelism coerces to sequential mapping`() = runTest {
        val result = (1..4).asFlow()
            .mapParallel(parallelism = -10) { it + 1 }
            .toList()

        result shouldBeEqualTo listOf(2, 3, 4, 5)
    }

    @Test
    fun `concurrent pipelines keep mapParallel bounded and isolated`() = runSuspendDefault(timeout = 15.seconds) {
        val workerCount = 8
        val pipelineCount = 32
        val parallelism = 4
        val inputs = (1..16).toList()
        val expected = inputs.map { it * it }.sorted()
        val completedPipelines = AtomicInteger(0)

        SuspendedJobTester()
            .workers(workerCount)
            .rounds(pipelineCount)
            .add {
                val inFlight = AtomicInteger(0)
                val peakInFlight = AtomicInteger(0)
                val twoTransformsEntered = CompletableDeferred<Unit>()

                val result = inputs.asFlow()
                    .mapParallel(parallelism) { value ->
                        val current = inFlight.incrementAndGet()
                        peakInFlight.updateAndGet { peak -> maxOf(peak, current) }
                        if (current >= 2) {
                            twoTransformsEntered.complete(Unit)
                        }

                        try {
                            twoTransformsEntered.await()
                            delay((value % 3 + 1).milliseconds)
                            value * value
                        } finally {
                            inFlight.decrementAndGet()
                        }
                    }
                    .toList()

                result.sorted() shouldBeEqualTo expected
                inFlight.get() shouldBeEqualTo 0
                peakInFlight.get() shouldBeGreaterThan 1
                peakInFlight.get() shouldBeLessThan parallelism + 1
                completedPipelines.incrementAndGet()
            }
            .run()

        completedPipelines.get() shouldBeEqualTo pipelineCount
    }
}

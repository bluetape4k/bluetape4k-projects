package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResultSet
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class MapParallelTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    private val parallelism = 4

    @Test
    fun `mapParallel with custom dispatcher`() = runTest {
        val dispatcher = newFixedThreadPoolContext(parallelism, "flowext")

        val ranges = flowRangeOf(1, 20)

        ranges
            .onEach { delay(10) }.log("source")
            .buffer()
            .mapParallel(parallelism = parallelism, context = dispatcher) {
                // log.trace { "map parallel: $it" }
                delay(Random.nextLong(10))
                it
            }
            .log("mapParallel")
            .assertResultSet(ranges.toFastList())
    }

    @Test
    fun `mapParallel with default dispatcher`() = runTest {
        val ranges = flowRangeOf(1, 20)

        ranges
            .onEach { delay(10) }.log("source")
            .buffer()
            .mapParallel(parallelism = parallelism, context = Dispatchers.Default) {
                // log.trace { "map parallel: $it" }
                delay(Random.nextLong(10))
                it
            }
            .log("mapParallel")
            .assertResultSet(ranges.toFastList())
    }

    @Test
    fun `mapParallel with exception`() = runTest {
        val ranges = flowRangeOf(1, 20)
        val error = RuntimeException("Boom!")

        assertFailsWith<RuntimeException> {
            ranges
                .log("source")
                .mapParallel(parallelism = parallelism, context = Dispatchers.Default) {
                    log.trace { "map parallel: $it" }
                    delay(Random.nextLong(10))
                    if (it == 3) throw error
                    else it
                }
                .log("mapParallel")
                .collect()
        }
    }
}

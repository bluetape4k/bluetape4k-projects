package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class TimeoutTest: AbstractFlowTest() {

    @Test
    fun `idle timeout starts at collection and resets after each item`() = runTest {
        val error = assertFailsWith<FlowTimeoutException> {
            flow {
                emit(1)
                delay(40.milliseconds)
                emit(2)
                delay(60.milliseconds)
                emit(3)
            }.timeout(50.milliseconds).toList()
        }

        error.timeout shouldBeEqualTo 50.milliseconds
    }

    @Test
    fun `timeout fallback is collected once after upstream cleanup`() = runTest {
        var cleaned = false
        var fallbackSubscriptions = 0
        val result = flow {
            try {
                emit(1)
                awaitCancellation()
            } finally {
                cleaned = true
            }
        }.timeoutOrFallback(
            50.milliseconds,
            flow {
                fallbackSubscriptions++
                emit(9)
                emit(10)
            },
        ).toList()

        cleaned shouldBeEqualTo true
        fallbackSubscriptions shouldBeEqualTo 1
        result shouldBeEqualTo listOf(1, 9, 10)
    }

    @Test
    fun `normal completion wins over pending timeout`() = runTest {
        flowOf(1, 2).timeout(1.hours).toList() shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun `upstream failure remains unchanged`() = runTest {
        assertFailsWith<IllegalStateException> {
            flow<Int> { throw IllegalStateException("upstream") }
                .timeout(1.hours)
                .collect()
        }
    }

    @Test
    fun `caller cancellation is not converted to timeout`() = runTest {
        val job = launch { flow<Int> { awaitCancellation() }.timeout(1.hours).collect() }
        job.cancelAndJoin()
        job.isCancelled shouldBeEqualTo true
    }

    @Test
    fun `invalid timeout is rejected`() = runTest {
        assertFailsWith<IllegalArgumentException> { flowOf(1).timeout(Duration.ZERO).toList() }
    }

    @Test
    fun `fallback failure remains unchanged`() = runTest {
        assertFailsWith<IllegalStateException> {
            flow<Int> { awaitCancellation() }
                .timeoutOrFallback(50.milliseconds, flow { throw IllegalStateException("fallback") })
                .toList()
        }
    }
}

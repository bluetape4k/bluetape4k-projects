package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class ConcatMapEagerBoundedTest: AbstractFlowTest() {

    @Test
    fun `bounded eager mapping preserves order and limits active inners`() = runTest {
        val active = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val result = flowRangeOf(1, 8)
            .concatMapEager(maxConcurrency = 2, bufferCapacity = 1) { value ->
                flow {
                    active.incrementAndGet()
                    peak.updateAndGet { maxOf(it, active.get()) }
                    try {
                        emit(value * 10)
                        delay(10.milliseconds)
                        emit(value * 10 + 1)
                    } finally {
                        active.decrementAndGet()
                    }
                }
            }.toList()

        result shouldBeEqualTo (1..8).flatMap { listOf(it * 10, it * 10 + 1) }
        peak.get() shouldBeLessOrEqualTo 2
        active.get() shouldBeEqualTo 0
    }

    @Test
    fun `bounded eager cancellation stops all inners`() = runTest {
        val cancelled = AtomicInteger(0)
        flowRangeOf(1, 20)
            .concatMapEager(maxConcurrency = 2, bufferCapacity = 1) {
                flow {
                    try {
                        emit(it)
                        awaitCancellation()
                    } finally {
                        cancelled.incrementAndGet()
                    }
                }
            }.take(1).toList()

        cancelled.get() shouldBeGreaterThan 0
    }

    @Test
    fun `bounded arguments fail before collection`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            flowOf(1).concatMapEager(maxConcurrency = 0) { flowOf(it) }.toList()
        }
        assertFailsWith<IllegalArgumentException> {
            flowOf(1).concatMapEager(maxConcurrency = 1, bufferCapacity = -1) { flowOf(it) }.toList()
        }
    }

    @Test
    fun `transform failure remains unchanged`() = runTest {
        assertFailsWith<IllegalStateException> {
            flowOf(1).concatMapEager<Int, Int>(maxConcurrency = 2) {
                throw IllegalStateException("transform")
            }.toList()
        }
    }

    @Test
    fun `inner failure remains unchanged`() = runTest {
        assertFailsWith<IllegalStateException> {
            flowOf(1).concatMapEager(maxConcurrency = 2) {
                flow { throw IllegalStateException("inner") }
            }.toList()
        }
    }
}

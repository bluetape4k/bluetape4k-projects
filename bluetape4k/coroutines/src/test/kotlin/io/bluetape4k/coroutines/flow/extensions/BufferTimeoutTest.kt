package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BufferTimeoutTest: AbstractFlowTest() {

    @Test
    fun `count boundary closes buffer before timeout`() = runTest {
        flowOf(1, 2, 3, 4, 5)
            .bufferTimeout(maxSize = 2, timeout = 1.hours)
            .toList() shouldBeEqualTo listOf(listOf(1, 2), listOf(3, 4), listOf(5))
    }

    @Test
    fun `virtual time closes a partial buffer`() = runTest {
        val values = flow {
            emit(1)
            delay(100.milliseconds)
            emit(2)
        }.bufferTimeout(maxSize = 10, timeout = 50.milliseconds).toList()

        values shouldBeEqualTo listOf(listOf(1), listOf(2))
    }

    @Test
    fun `completion emits one non empty partial buffer`() = runTest {
        flowOf(1, 2, 3)
            .bufferTimeout(maxSize = 10, timeout = 1.hours)
            .toList() shouldBeEqualTo listOf(listOf(1, 2, 3))
    }

    @Test
    fun `upstream failure drops in flight partial buffer`() = runTest {
        val emitted = mutableListOf<List<Int>>()
        val source = flow<Int> {
            emit(1)
            throw IllegalStateException("boom")
        }

        assertFailsWith<IllegalStateException> {
            source.bufferTimeout(10, 1.hours).collect { emitted += it }
        }
        emitted shouldBeEqualTo emptyList()
    }

    @Test
    fun `window timeout exposes repeatable cold windows`() = runTest {
        val windows = flowOf(1, 2, 3).windowTimeout(2, 1.hours).toList()

        windows.map { it.toList() } shouldBeEqualTo listOf(listOf(1, 2), listOf(3))
        windows.first().toList() shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun `invalid size and duration fail before collection`() = runTest {
        assertFailsWith<IllegalArgumentException> { flowOf(1).bufferTimeout(0, 1.seconds).toList() }
        assertFailsWith<IllegalArgumentException> { flowOf(1).bufferTimeout(1, Duration.ZERO).toList() }
    }

    @Test
    fun `receive wins a same instant count timeout tie`() = runTest {
        val values = flow {
            emit(1)
            delay(50.milliseconds)
            emit(2)
        }.bufferTimeout(2, 50.milliseconds).toList()

        values shouldBeEqualTo listOf(listOf(1, 2))
    }

    @Test
    fun `take cancellation closes the upstream producer`() = runTest {
        var cancelled = false
        flow {
            try {
                emit(1)
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }.bufferTimeout(10, 1.hours).take(1).collect()

        cancelled shouldBeEqualTo true
    }
}

package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import com.danrusu.pods4k.immutableArrays.toImmutableIntArray
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ImmutableChunkedTest {

    companion object: KLoggingChannel()

    @Test
    fun `chunk flow to immutable array`() = runTest {
        var chunkedCount = 0
        val chunkSize = 5

        flowRangeOf(1, 20).log("source")
            .immutableChunked(chunkSize).log("chunked")
            .onEach { chunkedCount++ }
            .test {
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3, 4, 5)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(6, 7, 8, 9, 10)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(11, 12, 13, 14, 15)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(16, 17, 18, 19, 20)
                awaitComplete()
            }

        chunkedCount shouldBeEqualTo 4
    }

    @Test
    fun `chunk flow to immutable array with partial window`() = runTest {
        var chunkedCount = 0
        val chunkSize = 3

        flowRangeOf(1, 10).log("source")
            .immutableChunked(chunkSize, true).log("chunked")
            .onEach { chunkedCount++ }
            .test {
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(4, 5, 6)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(7, 8, 9)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(10)      // partial window = true
                awaitComplete()
            }

        chunkedCount shouldBeEqualTo 4
    }

    @Test
    fun `flow 에 예외가 있으면 예외가 발생해야 합니다`() = runTest {
        flow<Int> { throw RuntimeException("Boom") }.log("source")
            .immutableChunked(3).log("chunked")
            .assertError<RuntimeException>()
    }

    @Test
    fun `chunked 중 cancellation 이 발생하면 flow 가 취소되어야 합니다`() = runTest {
        flowRangeOf(0, 16).log("source")
            .immutableChunked(4).log("chunked")
            .take(2)
            .test {
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(0, 1, 2, 3)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(4, 5, 6, 7)
                awaitComplete()
            }
    }

    @Test
    fun `chunked flow with mutable shared flow`() = runTest {
        val flow = MutableSharedFlow<Int>(extraBufferCapacity = 64)
        val results = mutableListOf<ImmutableArray<Int>>()

        flow.immutableChunked(3)
            .onEach {
                results += it
                if (it.toImmutableIntArray() == immutableArrayOf(1, 2, 3)) {
                    flow.tryEmit(4)
                    flow.tryEmit(5)
                    flow.tryEmit(6)
                }
            }.launchIn(this)

        yield()

        launch {
            flow.tryEmit(1)
            flow.tryEmit(2)
            flow.tryEmit(3)
        }
        yield()

        advanceUntilIdle()
        this.coroutineContext.cancelChildren()

        results.map { it.toImmutableIntArray() } shouldBeEqualTo listOf(
            immutableArrayOf(1, 2, 3),
            immutableArrayOf(4, 5, 6)
        )
    }
}

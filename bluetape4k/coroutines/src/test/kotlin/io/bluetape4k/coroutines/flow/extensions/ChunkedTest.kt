package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.collections.intRangeOf
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.logging.KLogging
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

class ChunkedTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `chunk flow`() = runTest {
        var chunkCount = 0
        val chunkSize = 5

        flowRangeOf(1, 20).log("source")
            .chunked(chunkSize).log("chunked")
            .onEach { chunkCount++ }
            .test {
                awaitItem() shouldBeEqualTo intRangeOf(1, 5)
                awaitItem() shouldBeEqualTo intRangeOf(6, 5)
                awaitItem() shouldBeEqualTo intRangeOf(11, 5)
                awaitItem() shouldBeEqualTo intRangeOf(16, 5)
                awaitComplete()
            }

        chunkCount shouldBeEqualTo 4
    }

    @Test
    fun `chunk flow with remaining`() = runTest {
        flowRangeOf(1, 10).log("source")
            .chunked(3).log("chunked")
            .test {
                awaitItem() shouldBeEqualTo listOf(1, 2, 3)
                awaitItem() shouldBeEqualTo listOf(4, 5, 6)
                awaitItem() shouldBeEqualTo listOf(7, 8, 9)
                awaitItem() shouldBeEqualTo listOf(10)
                awaitComplete()
            }
    }

    @Test
    fun `chunked flow - check with turbine`() = runTest {
        flowRangeOf(0, 10).log("source")
            .chunked(3).log("chunked")
            .test {
                awaitItem() shouldBeEqualTo listOf(0, 1, 2)
                awaitItem() shouldBeEqualTo listOf(3, 4, 5)
                awaitItem() shouldBeEqualTo listOf(6, 7, 8)
                awaitItem() shouldBeEqualTo listOf(9)
                awaitComplete()
            }
    }

    @Test
    fun `flow 에 예외가 있으면 예외가 발생합니다`() = runTest {
        flow<Int> { throw RuntimeException("Boom!") }.log("source")
            .chunked(3).log("chunked")
            .assertError<RuntimeException>()
    }

    @Test
    fun `chunked with cancellation`() = runTest {
        flowRangeOf(0, 10).log("source")
            .chunked(4).log("chunked")
            .take(2)
            .test {
                awaitItem() shouldBeEqualTo intRangeOf(0, 4)
                awaitItem() shouldBeEqualTo intRangeOf(4, 4)
                awaitComplete()
            }
    }

    @Test
    fun `chunked with mutable shared flow`() = runTest {
        val flow = MutableSharedFlow<Int>(extraBufferCapacity = 64)
        val results = mutableListOf<List<Int>>()

        flow.chunked(3)
            .onEach {
                results += it
                if (it == listOf(1, 2, 3)) {
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

        results shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(4, 5, 6))
    }
}

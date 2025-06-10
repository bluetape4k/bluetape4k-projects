package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class SlidingTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `sliding flow`() = runTest {
        val slidingCounter = atomic(0)
        val slidingCount by slidingCounter
        val slidingSize = 5

        val sliding = flowRangeOf(1, 20).log("source")
            .sliding(slidingSize).log("sliding")
            .onEach { slide ->
                slide.size shouldBeLessOrEqualTo slidingSize
                slidingCounter.incrementAndGet()
            }
            .toList()

        slidingCount shouldBeEqualTo 20
        sliding shouldHaveSize 20

        sliding.first() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
        sliding.last() shouldBeEqualTo listOf(20)
    }

    @Test
    fun `sliding flow with remaining`() = runTest {
        val slidingCounter = atomic(0)
        val slidingSize = 3

        val sliding = flowRangeOf(1, 20).log("source")
            .sliding(slidingSize).log("sliding")
            .onEach { slide ->
                slide.size shouldBeLessOrEqualTo slidingSize
                slidingCounter.incrementAndGet()
            }
            .toList()

        slidingCounter.value shouldBeEqualTo 20
        sliding shouldHaveSize 20

        sliding.first() shouldBeEqualTo listOf(1, 2, 3)
        sliding.last() shouldBeEqualTo listOf(20)
    }

    @Test
    fun `buffered sliding - 버퍼링을 하면서 sliding 합니다`() = runTest {
        val flow = flowOf(1, 2, 3, 4, 5)

        val sliding = flow.bufferedSliding(3).log("buffered sliding")

        sliding.test {
            awaitItem() shouldBeEqualTo listOf(1)
            awaitItem() shouldBeEqualTo listOf(1, 2)
            awaitItem() shouldBeEqualTo listOf(1, 2, 3)
            awaitItem() shouldBeEqualTo listOf(2, 3, 4)
            awaitItem() shouldBeEqualTo listOf(3, 4, 5)
            awaitItem() shouldBeEqualTo listOf(4, 5)
            awaitItem() shouldBeEqualTo listOf(5)
            awaitComplete()
        }
    }

    @Test
    fun `sliding with cancellation`() = runTest {
        flowRangeOf(0, 10)
            .sliding(4).log("sliding")
            .take(2)
            .test {
                awaitItem() shouldBeEqualTo listOf(0, 1, 2, 3)
                awaitItem() shouldBeEqualTo listOf(1, 2, 3, 4)
                awaitComplete()
            }
    }

    @Test
    fun `sliding with mutable shared flow`() = runTest {
        val flow = MutableSharedFlow<Int>(extraBufferCapacity = 64)
        val results = mutableListOf<List<Int>>()

        flow.sliding(3).log("job1")
            .onEach {
                results += it
                if (it == listOf(1, 2, 3)) {
                    flow.tryEmit(4).shouldBeTrue()
                }
            }
            .launchIn(this)
        yield()

        launch {
            flow.tryEmit(1).shouldBeTrue()
            flow.tryEmit(2).shouldBeTrue()
            flow.tryEmit(3).shouldBeTrue()
        }
        yield()
        advanceUntilIdle()

        this.coroutineContext.cancelChildren()

        results shouldBeEqualTo listOf(
            listOf(1, 2, 3),
            listOf(2, 3, 4)
        )
    }
}

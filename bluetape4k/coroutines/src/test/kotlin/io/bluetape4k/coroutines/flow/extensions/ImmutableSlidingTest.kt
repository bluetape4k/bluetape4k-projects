package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import com.danrusu.pods4k.immutableArrays.ImmutableIntArray
import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import com.danrusu.pods4k.immutableArrays.toImmutableArray
import com.danrusu.pods4k.immutableArrays.toImmutableIntArray
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
import java.util.concurrent.atomic.AtomicInteger

class ImmutableSlidingTest {

    companion object: KLoggingChannel()

    @Test
    fun `sliding flow to immutable array`() = runTest {
        val slidingCount = AtomicInteger(0)
        val slidingSize = 5

        val sliding = flowRangeOf(1, 20).log("source")
            .immutableSliding(slidingSize).log("sliding")
            .onEach { slide ->
                slide.size shouldBeLessOrEqualTo slidingSize
                slidingCount.incrementAndGet()
            }
            .toList()

        slidingCount.get() shouldBeEqualTo 20
        sliding shouldHaveSize 20

        sliding.first().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3, 4, 5)
        sliding.last().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(20)
    }

    @Test
    fun `sliding flow with partial window to immutable array`() = runTest {
        val slidingCount = AtomicInteger(0)
        val slidingSize = 3

        val sliding = flowRangeOf(1, 20).log("source")
            .immutableSliding(slidingSize).log("sliding")
            .onEach { slide ->
                slide.size shouldBeLessOrEqualTo slidingSize
                slidingCount.incrementAndGet()
            }
            .toList()

        slidingCount.get() shouldBeEqualTo 20
        sliding shouldHaveSize 20

        sliding.first().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3)
        sliding.last().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(20)
    }

    @Test
    fun `buffered sliding - 버퍼링을 하면서 sliding 합니다`() = runTest {
        val flow = flowOf(1, 2, 3, 4, 5)

        val sliding = flow.immutableBufferedSliding(3).log("buffered sliding")

        sliding.test {
            awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1)
            awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2)
            awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3)
            awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(2, 3, 4)
            awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(3, 4, 5)
            awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(4, 5)
            awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(5)
            awaitComplete()
        }
    }

    @Test
    fun `sliding with cancellation`() = runTest {
        flowRangeOf(0, 10).log("source")
            .immutableSliding(4).log("sliding")
            .take(2)
            .test {
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(0, 1, 2, 3)
                awaitItem().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3, 4)
                awaitComplete()
            }
    }

    @Test
    fun `sliding with mutable shared flow`() = runTest {
        val flow = MutableSharedFlow<Int>(extraBufferCapacity = 64)
        val results = mutableListOf<ImmutableIntArray>()

        flow.sliding(3).log("job1")
            .onEach {
                results += it.toImmutableArray()
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
            immutableArrayOf(1, 2, 3),
            immutableArrayOf(2, 3, 4)
        )
    }
}

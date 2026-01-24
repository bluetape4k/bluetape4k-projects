package io.bluetape4k.coroutines.flow.extensions

import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import com.danrusu.pods4k.immutableArrays.toImmutableIntArray
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ImmutableWindowedTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `window flow to immutable array`() = runTest {
        val windowedCount = AtomicInteger(0)
        val windowedSize = 5
        val windowedStep = 1

        val windowed = flowRangeOf(1, 20).log("source")
            .immutableWindowed(windowedSize, windowedStep).log("windowed")
            .onEach { windowed ->
                windowed.size shouldBeLessOrEqualTo windowedSize
                windowedCount.incrementAndGet()
            }
            .toImmutableArray()

        windowedCount.get() shouldBeEqualTo 16
        windowed.size shouldBeEqualTo 16
        windowed.first().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3, 4, 5)
        windowed.last().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(16, 17, 18, 19, 20)
    }

    @Test
    fun `window flow to immutable array with partial windowed`() = runTest {
        val windowedCount = AtomicInteger(0)
        val windowedSize = 5
        val windowedStep = 1

        val windowed = flowRangeOf(1, 20).log("source")
            .immutableWindowed(windowedSize, windowedStep, true).log("windowed")
            .onEach { windowed ->
                windowed.size shouldBeLessOrEqualTo windowedSize
                windowedCount.incrementAndGet()
            }
            .toImmutableArray()

        windowedCount.get() shouldBeEqualTo 20
        windowed.size shouldBeEqualTo 20
        windowed.first().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3, 4, 5)
        windowed.last().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(20)
    }

    @Test
    fun `window flow to immutable array distinct - like chunked`() = runTest {
        val windowedCount = AtomicInteger(0)
        val windowedSize = 5
        val windowedStep = 5

        val windowed = flowRangeOf(1, 20).log("source")
            .immutableWindowed(windowedSize, windowedStep).log("windowed")
            .onEach { windowed ->
                windowed.size shouldBeLessOrEqualTo windowedSize
                windowedCount.incrementAndGet()
            }
            .toImmutableArray()

        windowedCount.get() shouldBeEqualTo 4
        windowed.size shouldBeEqualTo 4
        windowed.first().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(1, 2, 3, 4, 5)
        windowed.last().toImmutableIntArray() shouldBeEqualTo immutableArrayOf(16, 17, 18, 19, 20)
    }
}

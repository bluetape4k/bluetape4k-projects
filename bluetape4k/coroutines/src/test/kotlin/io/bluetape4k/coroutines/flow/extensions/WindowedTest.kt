package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class WindowedTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Nested
    inner class Windowed {

        @Test
        fun `windowed flow`() = runTest {
            val windowedCount = AtomicInteger(0)
            val windowedSize = 5
            val windowedStep = 1

            val windowed = flowRangeOf(1, 20).log("source")
                .windowed(windowedSize, windowedStep).log("windowed")
                .onEach { windowed ->
                    windowed.size shouldBeLessOrEqualTo windowedSize
                    windowedCount.incrementAndGet()
                }
                .toFastList()

            windowedCount.get() shouldBeEqualTo 16
            windowed shouldHaveSize 16
            windowed.first() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
            windowed.last() shouldBeEqualTo listOf(16, 17, 18, 19, 20)
        }

        @Test
        fun `windowed flow with remaining`() = runTest {
            val windowedCounter = AtomicInteger(0)
            val windowedSize = 5
            val windowedStep = 1

            val windowed = flowRangeOf(1, 20).log("source")
                .windowed(windowedSize, windowedStep, true).log("windowed")
                .onEach { windowed ->
                    windowed.size shouldBeLessOrEqualTo windowedSize
                    windowedCounter.incrementAndGet()
                }
                .toFastList()

            windowedCounter.get() shouldBeEqualTo 20
            windowed shouldHaveSize 20
            windowed.first() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
            windowed.last() shouldBeEqualTo listOf(20)
        }

        @Test
        fun `windowed flow no duplicated`() = runTest {
            val windowedCounter = AtomicInteger(0)
            val windowedSize = 5
            val windowedStep = 5

            val windowed = flowRangeOf(1, 20).log("source")
                .windowed(windowedSize, windowedStep).log("windowed")
                .onEach { windowed ->
                    windowed.size shouldBeEqualTo windowedSize
                    windowedCounter.incrementAndGet()
                }
                .toFastList()

            windowedCounter.get() shouldBeEqualTo 4
            windowed shouldHaveSize 4
            windowed.first() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
            windowed.last() shouldBeEqualTo listOf(16, 17, 18, 19, 20)
        }
    }

    @Nested
    inner class WindowedFlow {
        @Test
        fun `windowed flow`() = runTest {
            val windowedCount = AtomicInteger(0)
            val windowedSize = 5
            val windowedStep = 1

            val windowed = flowRangeOf(1, 20).log("source")
                .windowedFlow(windowedSize, windowedStep).log("windowed")
                .onEach { windowed ->
                    val items = windowed.toList()
                    items.size shouldBeLessOrEqualTo windowedSize
                    windowedCount.incrementAndGet()
                }
                .toFastList()

            windowedCount.get() shouldBeEqualTo 16
            windowed shouldHaveSize 16
            windowed.first().toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
            windowed.last().toList() shouldBeEqualTo listOf(16, 17, 18, 19, 20)
        }

        @Test
        fun `windowed flow with remaining`() = runTest {
            val windowedCount = AtomicInteger(0)
            val windowedSize = 5
            val windowedStep = 1

            val windowed = flowRangeOf(1, 20).log("source")
                .windowedFlow(windowedSize, windowedStep, true).log("windowed")
                .onEach { windowed ->
                    val items = windowed.toList()
                    items.size shouldBeLessOrEqualTo windowedSize
                    windowedCount.incrementAndGet()
                }
                .toFastList()

            windowedCount.get() shouldBeEqualTo 20
            windowed shouldHaveSize 20
            windowed.first().toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
            windowed.last().toList() shouldBeEqualTo listOf(20)
        }

        @Test
        fun `windowed flow no duplicated`() = runTest {
            val windowedCount = AtomicInteger(0)
            val windowedSize = 5
            val windowedStep = 5

            val windowed = flowRangeOf(1, 20).log("source")
                .windowedFlow(windowedSize, windowedStep).log("windowed")
                .onEach { windowed ->
                    val items = windowed.toList()
                    items.size shouldBeEqualTo windowedSize
                    windowedCount.incrementAndGet()
                }
                .toFastList()

            windowedCount.get() shouldBeEqualTo 4
            windowed shouldHaveSize 4
            windowed.first().toList() shouldBeEqualTo listOf(1, 2, 3, 4, 5)
            windowed.last().toList() shouldBeEqualTo listOf(16, 17, 18, 19, 20)
        }
    }
}

package io.bluetape4k.coroutines.flow

import io.bluetape4k.collections.eclipse.fixedSizeList
import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import kotlin.random.Random

class AsyncFlowTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
        private const val ITEM_SIZE = 1_000
        private const val MIN_DELAY_TIME = 1L
        private const val MAX_DELAY_TIME = 10L

        private val expectedItems = fixedSizeList(ITEM_SIZE) { it + 1 }
    }

    @DisplayName("asyncFlow with default dispatcher")
    @RepeatedTest(REPEAT_SIZE)
    fun `asyncFlow with default dispatcher`() = runTest {
        runAsyncFlow(Dispatchers.Default)
    }

    @DisplayName("asyncFlow with IO dispatcher")
    @RepeatedTest(REPEAT_SIZE)
    fun `asyncFlow with io dispatcher`() = runTest {
        runAsyncFlow(Dispatchers.IO)
    }

    @DisplayName("asyncFlow with custom dispatcher")
    @RepeatedTest(REPEAT_SIZE)
    fun `asyncFlow with custom dispatcher`() = runTest {
        newFixedThreadPoolContext(Runtimex.availableProcessors, "asyncflow").use { dispatcher ->
            runAsyncFlow(dispatcher)
        }
    }

    @DisplayName("asyncFlow with VT dispatcher")
    @RepeatedTest(REPEAT_SIZE)
    fun `asyncFlow with virtual thread dispatcher`() = runTest {
        runAsyncFlow(Dispatchers.VT)
    }

    private suspend fun runAsyncFlow(dispatcher: CoroutineDispatcher) {
        // 중복된 요소가 없어야 합니다
        val results = mutableListOf<Int>()

        expectedItems
            .asFlow()
            .async(dispatcher) {
                delay(Random.nextLong(MIN_DELAY_TIME, MAX_DELAY_TIME))
                log.debug { "Started $it" }
                it
            }
            .log("#1")
            .collect { curr ->
                // 순차적으로 값을 받아야 합니다
                log.debug { "Collect $curr" }
                results.lastOrNull()?.let { prev -> curr shouldBeEqualTo prev + 1 }
                results.add(curr)
            }

        // 정렬된 값 그대로 Collect 되어야 합니다.
        results shouldBeEqualTo expectedItems
    }
}

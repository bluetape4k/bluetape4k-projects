package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class BufferingDebouncedTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    // NOTE: runTest 대신 runSuspendIO 를 사용해야 실제 Timer 가 동작합니다.

    @Test
    fun `debounced window 내에 발생한 모든 요소를 버퍼링하고, 디바운스 타이머가 만료되면 List로 발행합니다`() = runSuspendIO {
        val source = flow {
            emit(1)
            delay(110.milliseconds)
            emit(2)
            delay(95.milliseconds)
            emit(3)
            delay(100.milliseconds)
            emit(4)
            delay(80.milliseconds)
        }

        val buffered = source.bufferingDebounce(200.milliseconds)  // [1, 2], [3, 4]

        val itemLists = buffered.toList()
        log.debug { "itemLists=$itemLists" }

        itemLists shouldHaveSize 2 shouldBeEqualTo listOf(listOf(1, 2), listOf(3, 4))
    }

    @Test
    fun `flow 에서 예외를 발생 시키면, 그동안 버퍼링한 것들을 발행한다`() = runSuspendIO {
        val source =
            flow {
                emit(1)
                delay(150.milliseconds)
                emit(2)
                delay(150.milliseconds)
                emit(3)
                delay(150.milliseconds)

                throw RuntimeException("Boom!")

                delay(90.milliseconds)
                emit(4)
            }.catch { }

        val buffered = source.bufferingDebounce(200.milliseconds)  // [1, 2], [3]

        val itemLists = buffered.toList()
        log.debug { "itemLists=$itemLists" }

        itemLists shouldHaveSize 2 shouldBeEqualTo listOf(listOf(1, 2), listOf(3))
    }

    @Test
    fun `flow 예외는 버퍼를 발행한 뒤 전파한다`() = runSuspendIO {
        val source = flow {
            emit(1)
            delay(150.milliseconds)
            emit(2)
            throw RuntimeException("Boom!")
        }

        val itemLists = mutableListOf<List<Int>>()
        assertFailsWith<RuntimeException> {
            source.bufferingDebounce(200.milliseconds).collect { itemLists += it }
        }

        itemLists shouldBeEqualTo listOf(listOf(1, 2))
    }
}

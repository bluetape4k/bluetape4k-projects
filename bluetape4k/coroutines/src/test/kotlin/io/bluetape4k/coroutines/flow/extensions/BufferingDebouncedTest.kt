package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class BufferingDebouncedTest: AbstractFlowTest() {

    companion object: KLogging()

    // NOTE: runTest 대신 runSuspendTest 를 사용해야 Timer 가 동작합니다.

    @Test
    fun `debounced window 내에 발생한 모든 요소를 버퍼링하고, 디바운스 타이머가 만료되면 List로 발행합니다`() = runSuspendTest {
        val source = flow {
            emit(1)
            delay(110)
            emit(2)
            delay(90)
            emit(3)
            delay(110)
            emit(4)
            delay(90)
        }

        val buffered = source.bufferingDebounce(200.milliseconds)  // [1, 2], [3, 4]

        val itemLists = buffered.toList()
        log.debug { "itemLists=$itemLists" }

        itemLists shouldHaveSize 2 shouldBeEqualTo listOf(listOf(1, 2), listOf(3, 4))
    }

    @Test
    fun `flow 에서 예외를 발생 시키면, 그동안 버퍼링한 것들을 발행한다`() = runSuspendTest {
        val source =
            flow {
                emit(1)
                delay(110)
                emit(2)
                delay(90)
                emit(3)
                delay(110)
                throw RuntimeException("Boom!")
                delay(90)
                emit(4)
            }.catch { }

        val buffered = source.bufferingDebounce(200.milliseconds)  // [1, 2], [3]

        val itemLists = buffered.toList()
        log.debug { "itemLists=$itemLists" }

        itemLists shouldHaveSize 2 shouldBeEqualTo listOf(listOf(1, 2), listOf(3))
    }
}

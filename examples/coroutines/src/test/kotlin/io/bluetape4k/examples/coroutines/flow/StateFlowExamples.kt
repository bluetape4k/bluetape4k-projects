package io.bluetape4k.examples.coroutines.flow

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * 상태를 관리하는 [MutableStateFlow] 에 대한 예제
 */
class StateFlowExamples {

    companion object: KLogging()

    @Test
    fun `StateFlow 값 변화 관찰하기 - 상태 전파`() = runTest {
        val state = MutableStateFlow(1)
        val changeCounter1 = atomic(0)
        val changeCounter2 = atomic(0)

        // 상태가 변경되면, collect 를 수행합니다.
        launch {
            state
                .log("#1")
                .collect { changeCounter1.incrementAndGet() }
        }
        delay(10)
        state.value = 2

        delay(10)

        // 상태가 변경되면, collect 를 수행합니다.
        launch {
            state
                .log("#2")
                .collect { changeCounter2.incrementAndGet() }
        }
        delay(10)
        state.value = 3

        delay(10)

        // collector1 에서는 state 값이 1 -> 2 -> 3 으로 변경되었으므로 3번 호출됩니다.
        changeCounter1.value shouldBeEqualTo 3

        // collector2 에서는 state 값이 2->3으로 변경되었으므로 2번 호출됩니다.
        changeCounter2.value shouldBeEqualTo 2

        // 자식 Job 들을 모두 취소한다 
        coroutineContext.cancelChildren()
    }

    @Test
    fun `stateIn - 일반 flow 를 StateFlow로 변환하기`() = runTest {
        val flow = flowOf("A", "B", "C")
            .onEach { delay(100) }
            .log("source")

        // stateIn 을 이용하여 일반 Flow를 StateFlow로 변환합니다.
        val stateFlow = flow.stateIn(this)

        log.info { "Listening" }
        log.info { "State=${stateFlow.value}" }

        val receivedCounter = atomic(0)

        launch {
            stateFlow
                .log("collector")
                .collect { receivedCounter.incrementAndGet() }
        }

        delay(500)
        log.info { "State=${stateFlow.value}" }

        // state 가 A -> B -> C 로 3번 변경되었다 
        receivedCounter.value shouldBeEqualTo 3

        coroutineContext.cancelChildren()
    }
}

package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ConcatArrayEagerTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `concat two flow items eagerly`() = runTest {
        val state1 = atomic(0)
        val state2 = atomic(0)

        val flow1 = flowRangeOf(1, 5).log("#1")
            .onStart {
                delay(200)
                state1.value = 1
            }

        val flow2 = flowRangeOf(6, 5).log("#2")
            .onStart { state2.value = state1.value }

        concatArrayEager(flow1, flow2)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        state1.value shouldBeEqualTo 1
        state2.value shouldBeEqualTo 0  // flow2 부터 consume 한다
    }

    @Test
    fun `concat one flow`() = runTest {
        concatArrayEager(flowRangeOf(1, 5).log("#1"))
            .assertResult(1, 2, 3, 4, 5)
    }

    @Test
    fun `concat with take`() = runTest {
        concatArrayEager(
            flowRangeOf(1, 5).onStart { delay(100) }.log("#1"),
            flowRangeOf(6, 5).log("#2"),
        )
            .take(6)
            .assertResult(1, 2, 3, 4, 5, 6)     // 첫 번째 flow 부터 collect 한다 
    }

    @Test
    fun `cancel concat`() = runTest {
        var counter = 0

        concatArrayEager(
            flowRangeOf(1, 5).log("#1")
                .onEach {
                    delay(200)
                    counter++
                }
        )
            .take(3)
            .assertResult(1, 2, 3)

        counter shouldBeEqualTo 3
    }

    @Test
    fun `concat list of flows`() = runTest {
        listOf(
            flowRangeOf(1, 5).onStart { delay(100) }.log("#1"),
            flowRangeOf(6, 5).log("#2"),
        )
            .concatFlows()
            .take(6)
            .assertResult(1, 2, 3, 4, 5, 6)     // 첫 번째 flow 부터 collect 한다
    }

    @Test
    fun `concat flow of flows`() = runTest {
        flowOf(
            flowRangeOf(1, 5).onStart { delay(100) }.log("#1"),
            flowRangeOf(6, 5).log("#2"),
        )
            .concatFlows()
            .take(6)
            .assertResult(1, 2, 3, 4, 5, 6)     // 첫 번째 flow 부터 collect 한다
    }
}

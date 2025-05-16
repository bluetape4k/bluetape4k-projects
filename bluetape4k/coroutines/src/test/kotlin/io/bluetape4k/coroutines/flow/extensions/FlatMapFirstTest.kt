package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FlatMapFirstTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `flatMapFirst for simple flow`() = runTest {
        flowOf("one", "two").log("s")
            .flatMapFirst { v ->
                flow {
                    delay(10L)
                    emit(v)
                }.log("t")
            }
            .log("flatMapFirst")
            .assertResult("one")
    }

    @Test
    fun `flatMapFirst for range`() = runTest {
        flowRangeOf(1, 10)
            .onEach { delay(100) }.log("src")
            .flatMapFirst {
                log.trace { "source item=$it" }
                flowRangeOf(it * 100, 5).onEach { delay(20) }.log("t")
            }
            .log("fmf")
            .assertResult(
                100, 101, 102, 103, 104,
                300, 301, 302, 303, 304,
                500, 501, 502, 503, 504,
                700, 701, 702, 703, 704,
                900, 901, 902, 903, 904
            )
    }

    @Test
    fun `flat map first with take`() = runTest {
        val item = atomic(0)

        // flatMapFirst 동작 시 collect 작업 후에 emit 된 것 중 가장 최신 것만 선택한다
        flowRangeOf(1, 10)
            .onEach { delay(100) }.log("source")
            .flatMapFirst {
                item.value = it
                flowRangeOf(it * 100, 5)
                    .onEach { delay(30) }.log("iner")
            }
            .take(7).log("take")
            .assertResult(
                100, 101, 102, 103, 104,
                300, 301,
            )  // 200 이 없는 이유는 1 -> 100 은 flatMapFirst에서 작업할 때, 2, 3이 송출되고, 가장 최근인 300 대가 송출된다

        item.value shouldBeEqualTo 3
    }

    @Test
    fun `flattenFirst for take first item`() = runTest {
        val flow1 = flow {
            delay(10L)
            emit("one")
        }
        val flow2 = flow {
            delay(20L)
            emit("two")
        }
        flowOf(flow1, flow2)
            .flattenFirst()
            .assertResult("one")
    }
}

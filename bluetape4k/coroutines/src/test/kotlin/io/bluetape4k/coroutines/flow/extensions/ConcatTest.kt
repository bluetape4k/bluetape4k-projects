package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test


class ConcatTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    val flow1 = flowOf(1, 2).log("#1")
    val flow2 = flowOf(3, 4).log("#2")
    val flow3 = flowOf(5, 6).log("#3")

    @Test
    fun `두 개의 Flow를 합친다`() = runTest {
        concat(flow1, flow2)
            .assertResult(1, 2, 3, 4)
    }

    @Test
    fun `세 개의 Flow를 합친다`() = runTest {
        concat(flow1, flow2, flow3)
            .assertResult(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `concatWith 로 합친다`() = runTest {
        flow1.concatWith(flow2)
            .assertResult(1, 2, 3, 4)

        flow1.concatWith(flow2, flow3)
            .assertResult(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `flow collection을 합친다`() = runTest {
        listOf(flow1, flow2, flow3)
            .concat()
            .assertResult(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `startWith items`() = runTest {
        flow1.startWith(0)
            .assertResult(0, 1, 2)

        flow1.startWith(-2, -1, 0)
            .assertResult(-2, -1, 0, 1, 2)
    }

    @Test
    fun `startWith with valueSupplier`() = runTest {
        var i = 1
        var called = false
        val flow = flowOf(2)
            .startWith {
                called = true
                i++
            }

        // flow 정의만 했지 Consume 하지는 않았다.
        called.shouldBeFalse()

        flow.assertResult(1, 2)  // i = 1
        called.shouldBeTrue()

        flow.assertResult(2, 2)  // i = 2
        flow.assertResult(3, 2)  // i = 3
    }

    @Test
    fun `다른 flow를 먼저 emit 한다`() = runTest {
        flow3.startWith(flow1, flow2)
            .assertResult(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `현 flow를 모두 emit하고 나서 주어진 item을 emit 한다`() = runTest {
        flow1.endWith(3)
            .assertResult(1, 2, 3)

        flow1.endWith(3, 4)
            .assertResult(1, 2, 3, 4)
    }

    @Test
    fun `현 flow를 모두 emit 하고, 주어진 flows 를 emit 한다`() = runTest {
        // concatWith 와 같다
        flow1.endWith(flow2)
            .assertResult(1, 2, 3, 4)

        flow1.endWith(flow2, flow3)
            .assertResult(1, 2, 3, 4, 5, 6)
    }
}

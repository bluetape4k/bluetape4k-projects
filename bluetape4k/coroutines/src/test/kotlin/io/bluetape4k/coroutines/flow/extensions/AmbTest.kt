package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertEmpty
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class AmbTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `amb with two flows`() = runTest {
        val flow1 = flowRangeOf(1, 5).onStart { delay(1000) }.log("#1")
        val flow2 = flowRangeOf(6, 5).onStart { delay(100) }.log("#2")

        amb(flow1, flow2)
            .assertResult(6, 7, 8, 9, 10)
    }

    @Test
    fun `amb with two flows 2`() = runTest {
        val flow1 = flowRangeOf(1, 5).onStart { delay(100) }.log("#1")
        val flow2 = flowRangeOf(6, 5).onStart { delay(1000) }.log("#2")

        amb(flow1, flow2)
            .assertResult(1, 2, 3, 4, 5)
    }

    @Test
    fun `amb with take`() = runTest {
        var counter = 0

        val flow1 = flowRangeOf(1, 5).onEach { delay(100) }.log("#1")
        val flow2 = flowRangeOf(6, 5)
            .onEach {
                delay(200)
                counter++
            }
            .log("#2")

        flow1.ambWith(flow2)
            .take(3)
            .assertResult(1, 2, 3)

        counter shouldBeEqualTo 0
    }

    @Test
    fun `amb ignores empty flow and waits for first emitted value`() = runTest {
        val empty = emptyFlow<Int>().onStart { delay(10) }
        val delayed = flowRangeOf(1, 3).onStart { delay(50) }

        amb(empty, delayed).assertResult(1, 2, 3)
    }

    @Test
    fun `amb returns empty when every source is empty`() = runTest {
        val empty1 = emptyFlow<Int>()
        val empty2 = emptyFlow<Int>().onStart { delay(10) }

        amb(empty1, empty2).assertEmpty()
    }
}

package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DeferTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `요소 발행을 지연합니다`() = runTest {
        var count = 0L
        val flow = defer {
            delay(count)
            flowOf(count)
        }.log("defer")

        flow.assertResult(count)

        flow.assertResult(++count)

        flow.assertResult(++count)
    }

    @Test
    fun `defer 내부 flow에서 예외를 emit하는 경우 예외가 전파되어야 한다`() = runTest {
        val exception = FlowOperationException("Boom!")

        defer { flow<Int> { throw exception }.log("#1") }
            .assertError<FlowOperationException>()

        defer { flow<Int> { throw exception }.log("#2") }
            .materialize()
            .assertResult(FlowEvent.Error(exception))
    }

    @Test
    fun `defer 내부 전체가 예외 시 예외가 전파된다`() = runTest {
        val exception = FlowOperationException("Boom!")

        defer<Int> { throw exception }
            .assertError<FlowOperationException>()
    }
}

package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FlowFromSuspendTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `flow from suspend emits values`() = runTest {
        var count = 1L
        val flow = flowFromSuspend {
            delay(count)
            count
        }

        flow.assertResult(count)

        flow.assertResult(++count)

        flow.assertResult(++count)
    }

    @Test
    fun `flow from suspend with exception`() = runTest {
        val exception = RuntimeException("Boom!")

        flowFromSuspend<Int> { throw exception }
            .assertError<RuntimeException>()

        flowFromSuspend<Int> { throw exception }
            .materialize()
            .assertResult(FlowEvent.Error(exception))
    }

    @Test
    fun `flow from suspend with value supplier raise exception`() = runTest {
        val exception = RuntimeException("Boom!")

        flowFromSuspend<Int> { throw exception }
            .assertError<RuntimeException>()
    }
}

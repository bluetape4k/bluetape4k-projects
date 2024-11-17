package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FlowFromSupplierTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `flow from function emits values`() = runTest {
        var count = 1L
        val flow = flowFromSupplier {
            Thread.sleep(count)
            count
        }

        flow.assertResult(count)

        flow.assertResult(++count)

        flow.assertResult(++count)
    }

    @Test
    fun `flow from function with exception`() = runTest {
        val exception = RuntimeException("Boom!")

        flowFromSupplier<Int> { throw exception }
            .assertError<RuntimeException>()

        flowFromSupplier<Int> { throw exception }
            .materialize()
            .assertResult(FlowEvent.Error(exception))
    }

    @Test
    fun `flow from function with value supplier raise exception`() = runTest {
        val exception = RuntimeException("Boom!")

        flowFromSupplier<Int> { throw exception }
            .assertError<RuntimeException>()
    }
}

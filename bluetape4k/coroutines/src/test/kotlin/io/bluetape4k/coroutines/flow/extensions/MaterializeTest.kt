package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MaterializeTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `materialize happy case`() = runTest {
        flowRangeOf(1, 3)
            .materialize()
            .test {
                awaitItem() shouldBeEqualTo FlowEvent.Value(1)
                awaitItem() shouldBeEqualTo FlowEvent.Value(2)
                awaitItem() shouldBeEqualTo FlowEvent.Value(3)
                awaitItem() shouldBeEqualTo FlowEvent.Complete
                awaitComplete()
            }
    }

    @Test
    fun `materialize with exception`() = runTest {
        val exception = RuntimeException("Boom!")

        flowRangeOf(1, 3)
            .concatWith(flow { throw exception })
            .materialize()
            .test {
                awaitItem() shouldBeEqualTo FlowEvent.Value(1)
                awaitItem() shouldBeEqualTo FlowEvent.Value(2)
                awaitItem() shouldBeEqualTo FlowEvent.Value(3)
                awaitItem() shouldBeEqualTo FlowEvent.Error(exception)
                awaitComplete()
            }

        flowRangeOf(1, 3)
            .startWith(flow { throw exception })
            .materialize()
            .test {
                awaitItem() shouldBeEqualTo FlowEvent.Error(exception)
                awaitComplete()
            }

        flowRangeOf(1, 3)
            .concatWith(flow { throw exception })
            .concatWith(flowOf(4, 5, 6))
            .materialize()
            .test {
                awaitItem() shouldBeEqualTo FlowEvent.Value(1)
                awaitItem() shouldBeEqualTo FlowEvent.Value(2)
                awaitItem() shouldBeEqualTo FlowEvent.Value(3)
                awaitItem() shouldBeEqualTo FlowEvent.Error(exception)
                awaitComplete()
            }
    }

    @Test
    fun `materialize with cancel`() = runTest {
        flowRangeOf(1, 3)
            .take(1)
            .materialize()
            .test {
                awaitItem() shouldBeEqualTo FlowEvent.Value(1)
                awaitItem() shouldBeEqualTo FlowEvent.Complete
                awaitComplete()
            }

        val exception = RuntimeException("Boom!")
        flowRangeOf(1, 3)
            .concatWith(flow { throw exception })
            .take(3)
            .materialize()
            .test {
                awaitItem() shouldBeEqualTo FlowEvent.Value(1)
                awaitItem() shouldBeEqualTo FlowEvent.Value(2)
                awaitItem() shouldBeEqualTo FlowEvent.Value(3)
                awaitItem() shouldBeEqualTo FlowEvent.Complete
                awaitComplete()
            }
    }
}

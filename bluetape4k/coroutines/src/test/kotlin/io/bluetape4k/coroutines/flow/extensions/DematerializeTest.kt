package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertFailure
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DematerializeTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `dematerialize flow`() = runTest {
        flowOf(1, 2, 3).log("s")
            .materialize().log("m")
            .dematerialize().log("d")
            .assertResult(1, 2, 3)

        flowOf(
            FlowEvent.Value(1),
            FlowEvent.Value(2),
            FlowEvent.Value(3),
        )
            .dematerialize()
            .assertResult(1, 2, 3)

        flowOf(
            FlowEvent.Value(1),
            FlowEvent.Value(2),
            FlowEvent.Value(3),
            FlowEvent.Complete,
            FlowEvent.Value(4),
            FlowEvent.Value(5),
            FlowEvent.Value(6),
        ).log("s")
            .dematerialize().log("d")
            .assertResult(1, 2, 3)
    }

    @Test
    fun `dematerialize Event Complete`() = runTest {
        flowOf(FlowEvent.Complete).dematerialize().test {
            awaitComplete()
        }
    }

    @Test
    fun `dematerialize Event of Nothing`() = runTest {
        emptyFlow<FlowEvent<Nothing>>().dematerialize().test {
            awaitComplete()
        }
    }

    @Test
    fun `dematerialize with exception`() = runTest {
        val ex = RuntimeException("Boom!")

        flowOf(1, 2, 3)
            .concatWith(flow { throw ex })
            .materialize()
            .dematerialize()
            .assertFailure<Int, RuntimeException>(1, 2, 3)

        flowOf(1, 2, 3)
            .startWith(flow { throw ex })
            .materialize()
            .dematerialize()
            .assertFailure<Int, RuntimeException>()

        flowOf(1, 2, 3)
            .concatWith(flow { throw ex }, flowOf(4, 5, 6))
            .materialize()
            .dematerialize()
            .assertFailure<Int, RuntimeException>(1, 2, 3)
    }

    @Test
    fun `dematerialize first item is Event Error`() = runTest {
        val ex = FlowOperationException("Boom!")

        flowOf(FlowEvent.Error(ex))
            .dematerialize()
            .assertError<FlowOperationException>()


        flowOf(FlowEvent.Error(ex), FlowEvent.Value(1))
            .dematerialize()
            .assertError<FlowOperationException>()

        flowOf(FlowEvent.Error(ex), FlowEvent.Complete)
            .dematerialize()
            .assertError<FlowOperationException>()
    }
}

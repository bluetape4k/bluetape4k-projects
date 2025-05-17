package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MapIndexedTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `mapIndexed simple usecase`() = runTest {
        flowRangeOf(1, 4)
            .mapIndexed { index, value -> index to value }
            .assertResult(
                0 to 1,
                1 to 2,
                2 to 3,
                3 to 4
            )
    }

    @Test
    fun `when upstream error`() = runTest {
        val ex = RuntimeException("Boom!")

        flowOf(ex)
            .mapIndexed { index, value -> index to value }
            .assertError<RuntimeException>()

        flow<Int> { throw ex }
            .mapIndexed { index, value -> index to value }
            .assertError<RuntimeException>()

        flowRangeOf(1, 2)
            .concatWith(flow { throw ex })
            .mapIndexed { index, value -> index to value }
            .test {
                awaitItem() shouldBeEqualTo Pair(0, 1)
                awaitItem() shouldBeEqualTo Pair(1, 2)
                awaitError()
            }
    }

    @Test
    fun `when cancel flow`() = runTest {
        channelFlow {
            repeat(5) {
                if (it == 2) throw CancellationException("")
                else send(it)
            }
        }
            .mapIndexed { index, value -> index to value }
            .take(2)
            .test {
                awaitItem() shouldBeEqualTo Pair(0, 0)
                awaitItem() shouldBeEqualTo Pair(1, 1)
                awaitComplete()
            }


        flowRangeOf(0, 6)
            .mapIndexed { index, value -> index to value }
            .take(2)
            .test {
                awaitItem() shouldBeEqualTo Pair(0, 0)
                awaitItem() shouldBeEqualTo Pair(1, 1)
                awaitComplete()
            }
    }
}

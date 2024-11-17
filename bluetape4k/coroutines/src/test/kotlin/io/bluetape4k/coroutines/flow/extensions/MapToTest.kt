package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MapToTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `mapTo basic`() = runTest {
        flowOf(1, 2, 3)
            .mapTo(4)
            .assertResult(4, 4, 4)
    }

    @Test
    fun `mapToUnit basic`() = runTest {
        flowOf(1, 2, 3)
            .mapToUnit()
            .assertResult(Unit, Unit, Unit)
    }

    @Test
    fun `when mapTo upstream error`() = runTest {
        val error = RuntimeException("Boom!")

        flow<Nothing> { throw error }
            .mapTo(2)
            .assertError<RuntimeException>()

        flowOf(1, 2)
            .concatWith(flow { throw error })
            .mapTo(2)
            .test {
                awaitItem() shouldBeEqualTo 2
                awaitItem() shouldBeEqualTo 2
                awaitError()
            }
    }

    @Test
    fun `when mapToUnit upstream error`() = runTest {
        val error = RuntimeException("Boom!")

        flow<Nothing> { throw error }
            .mapToUnit()
            .assertError<RuntimeException>()

        flowOf(1, 2)
            .concatWith(flow { throw error })
            .mapToUnit()
            .test {
                awaitItem() shouldBeEqualTo Unit
                awaitItem() shouldBeEqualTo Unit
                awaitError()
            }
    }
}

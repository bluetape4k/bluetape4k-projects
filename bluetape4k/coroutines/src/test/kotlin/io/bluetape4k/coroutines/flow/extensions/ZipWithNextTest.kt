package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ZipWithNextTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `zipWithNext basic`() = runTest {
        flowRangeOf(0, 4)
            .zipWithNext()
            .test {
                awaitItem() shouldBeEqualTo Pair(0, 1)
                awaitItem() shouldBeEqualTo Pair(1, 2)
                awaitItem() shouldBeEqualTo Pair(2, 3)
                awaitComplete()
            }

        flowRangeOf(0, 4)
            .sliding(2)
            .mapNotNull {
                if (it.size < 2) null
                else it[0] to it[1]
            }
            .test {
                awaitItem() shouldBeEqualTo Pair(0, 1)
                awaitItem() shouldBeEqualTo Pair(1, 2)
                awaitItem() shouldBeEqualTo Pair(2, 3)
                awaitComplete()
            }
    }

    @Test
    fun `zipWithNext nullable`() = runTest {
        // 0 - null - 2 - null
        flowRangeOf(0, 4)
            .map { it.takeIf { it % 2 == 0 } }
            .zipWithNext()
            .test {
                awaitItem() shouldBeEqualTo (0 to null)
                awaitItem() shouldBeEqualTo (null to 2)
                awaitItem() shouldBeEqualTo (2 to null)
                awaitComplete()
            }
    }

    @Test
    fun `zipWithNext with empty flow`() = runTest {
        emptyFlow<Int>()
            .zipWithNext()
            .test {
                awaitComplete()
            }
    }

    @Test
    fun `pairwise with single value flow`() = runTest {
        flowOf(1)
            .pairwise()
            .test {
                awaitComplete()
            }
    }

    @Test
    fun `zipWithNext with failure upstream`() = runTest {
        flow<Int> { throw RuntimeException("Boom!") }
            .zipWithNext()
            .assertError<RuntimeException>()
    }

    @Test
    fun `zipWithNext with cancellation`() = runTest {
        flowRangeOf(1, 100)
            .zipWithNext()
            .take(2)
            .test {
                awaitItem() shouldBeEqualTo (1 to 2)
                awaitItem() shouldBeEqualTo (2 to 3)
                awaitComplete()
            }
    }
}

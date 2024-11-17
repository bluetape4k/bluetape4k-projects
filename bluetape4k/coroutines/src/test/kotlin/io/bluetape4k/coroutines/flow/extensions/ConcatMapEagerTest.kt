package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertFailure
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ConcatMapEagerTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `concat map eagerly`() = runTest {
        flowRangeOf(1, 5).log("source")
            .concatMapEager {
                flowRangeOf(it * 10, 5).onEach { delay(100) }.log("inner")
            }
            .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31, 32, 33, 34,
                40, 41, 42, 43, 44,
                50, 51, 52, 53, 54
            )
    }

    @Test
    fun `concat map with take`() = runTest {
        flowRangeOf(1, 5).log("source")
            .concatMapEager {
                flowRangeOf(it * 10, 5).onEach { delay(100) }.log("inner")
            }
            .take(7)                // cancel 
            .assertResult(
                10, 11, 12, 13, 14,
                20, 21
            )
    }

    @Test
    fun `concat map with crash mapper`() = runTest {
        flowRangeOf(1, 5)
            .concatMapEager<Int, Int> {
                throw RuntimeException("Boom!")
            }
            .assertFailure<Int, RuntimeException>()
    }

    @Test
    fun `concat map with crash in flow`() = runTest {
        flowRangeOf(1, 5)
            .concatMapEager<Int, Int> {
                flow {
                    throw RuntimeException("Boom!")
                }
            }
            .assertFailure<Int, RuntimeException>()
    }
}

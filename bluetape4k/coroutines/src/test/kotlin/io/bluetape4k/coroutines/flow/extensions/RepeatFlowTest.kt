package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RepeatFlowTest: AbstractFlowTest() {

    companion object: KLogging()

    private val testDispatcher = newFixedThreadPoolContext(8, "flowx")

    @Test
    fun `repeatFlow operator`() = runTest {
        var result = 42
        repeatFlow(4) { result++ }
            .onEach { delay(Random.nextLong(5)) }
            .log("repeat", log)
            .assertResult(result, result + 1, result + 2, result + 3)
    }

    @Test
    fun `repeatFlow with exception`() = runTest {
        var result = 42
        repeatFlow(4) {
            if (it == 2) throw RuntimeException("Boom!")
            else result++
        }.log("repeat")
            .test {
                awaitItem() shouldBeEqualTo 42
                awaitItem() shouldBeEqualTo 43
                awaitError() shouldBeInstanceOf RuntimeException::class
            }
    }

    @Test
    fun `repeatFlow with cancellation before flowOn`() = runTest {
        val initValue = 42
        var currValue = initValue
        repeatFlow(4) {
            if (it == 2) throw RuntimeException("Boom!")
            else currValue++
        }
            .log("repeat")
            .take(2)                // cancellation before flowOn
            .flowOn(testDispatcher)
            .test {
                awaitItem() shouldBeEqualTo initValue
                awaitItem() shouldBeEqualTo initValue + 1
                awaitComplete()
            }
    }

    @Test
    fun `repeatFlow with cancellation after flowOn`() = runTest {
        val initValue = 42
        var currValue = initValue
        repeatFlow(4) {
            if (it == 2) throw RuntimeException("Boom!")
            else currValue++
        }
            .log("repeat")
            .flowOn(testDispatcher)
            .take(2)            // cancellation after flowOn
            .test {
                awaitItem() shouldBeEqualTo initValue
                awaitItem() shouldBeEqualTo initValue + 1
                awaitError() shouldBeInstanceOf RuntimeException::class
            }
    }
}

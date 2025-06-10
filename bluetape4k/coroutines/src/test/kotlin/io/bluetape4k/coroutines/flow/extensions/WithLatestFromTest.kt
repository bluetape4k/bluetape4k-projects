package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class WithLatestFromTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `with other latest item`() = runTest {
        val f1 = flowOf(1, 2, 3, 4).log("f1")
        val f2 = flowOf("a", "b", "c", "d", "e").log("f2")

        f2.withLatestFrom(f1)
            .assertResult(
                "a" to 4,
                "b" to 4,
                "c" to 4,
                "d" to 4,
                "e" to 4
            )
    }

    @Test
    fun `with other latest item which is null`() = runTest {
        val f1 = flowOf(1, 2, 3, 4, null).log("f1")
        val f2 = flowOf("a", "b", "c", "d", "e").log("f2")

        f2.withLatestFrom(f1)
            .assertResult("a" to null, "b" to null, "c" to null, "d" to null, "e" to null)
    }

    @Test
    fun `with other latest with delay`() = runTest {
        val f1 = flowOf(1, 2, 3, 4).onEach { delay(300) }.log("f1")
        val f2 = flowOf("a", "b", "c", "d", "e", "f").onEach { delay(100) }.log("f2")

        f2.withLatestFrom(f1)
            .assertResult("c" to 1, "d" to 1, "e" to 1, "f" to 2)
    }

    @Test
    fun `withLatestFrom with failure upstream`() = runTest {

        flow<Int> { throw RuntimeException("Boom!") }
            .withLatestFrom(neverFlow())
            .assertError<RuntimeException>()

        neverFlow()
            .withLatestFrom(flow<Int> { throw RuntimeException("Boom!") })
            .assertError<RuntimeException>()
    }

    @Test
    fun `withLatestFrom with source cancellation`() = runTest {
        flow {
            emit(1)
            throw kotlinx.coroutines.CancellationException("Boom!")
        }.log("source")
            .withLatestFrom(emptyFlow<Nothing>()).log("latest")
            .assertError<kotlinx.coroutines.CancellationException>()
    }

    @Test
    fun `withLatestFrom with inner cancellation`() = runTest {
        flowOf(1).log("source")
            .withLatestFrom(
                flow {
                    emit("a")
                    throw kotlinx.coroutines.CancellationException("Boom!")
                }.log("inner")
            ).log("latest")
            .test {
                awaitItem() shouldBeEqualTo Pair(1, "a")
                awaitComplete()
            }
    }
}

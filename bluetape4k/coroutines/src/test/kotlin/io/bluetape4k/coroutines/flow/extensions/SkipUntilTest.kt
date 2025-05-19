package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SkipUntilTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `skip until`() = runTest {
        // -------1-------2-------3
        // -----------|
        flowOf(1, 2, 3)
            .log("source")
            .onEach { delay(100) }
            .skipUntil(delayedFlow(150)).log("skipUntil")
            .assertResult(2, 3)

        // dropUntil 은 skipUntil 과 같은 기능을 합니다.
        flowOf(1, 2, 3)
            .log("source")
            .onEach { delay(100) }
            .dropUntil(delayedFlow(150)).log("dropUntil")
            .assertResult(2, 3)
    }

    @Test
    fun `skip until with never flow`() = runTest {
        flowOf(1, 2, 3, 4)
            .log("source")
            .skipUntil(neverFlow()).log("skipUntil")
            .assertResult()
    }

    @Test
    fun `skip until with empty flow`() = runTest {
        flowOf(1, 2, 3, 4)
            .log("source")
            .skipUntil(emptyFlow()).log("skipUntil")
            .assertResult(1, 2, 3, 4)
    }

    @Test
    fun `skip until with failure upstream`() = runTest {
        // 01-------------2X
        // -------100
        val source = flow {
            emit(0)
            emit(1)

            delay(20)
            emit(2)
            throw RuntimeException("Boom!")
        }

        val notifier = flowOf(100).onEach { delay(10) }.log("notifier")

        source
            .log("source")
            .buffer()
            .skipUntil(notifier)
            .onEach {
                it shouldBeEqualTo 2
            }
            .test {
                awaitItem() shouldBeEqualTo 2
                awaitError() shouldBeInstanceOf RuntimeException::class
            }
    }
}

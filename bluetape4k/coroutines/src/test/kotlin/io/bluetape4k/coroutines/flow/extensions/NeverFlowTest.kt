package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NeverFlowTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `compare neverFlow same instance`() {
        neverFlow() shouldBeEqualTo neverFlow()
        neverFlow() shouldBeEqualTo NeverFlow
        NeverFlow.Companion shouldBeEqualTo NeverFlow
    }

    @Test
    fun `neverFlow example`() = runTest(timeout = 2.seconds) {
        val itemSize = 1_000
        val list = mutableListOf<Any?>()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            neverFlow().toList(list)
        }

        val intervalJob = intervalFlowOf(Duration.ZERO, 100.milliseconds)
            .log("interval")
            .take(itemSize)
            .launchIn(this)

        runCurrent()

        repeat(itemSize) {
            advanceTimeBy(itemSize / 10L)
            runCurrent()
            list.shouldBeEmpty()
        }

        advanceUntilIdle()
        intervalJob.cancelAndJoin()
        job.cancelAndJoin()

        list.shouldBeEmpty()
    }
}

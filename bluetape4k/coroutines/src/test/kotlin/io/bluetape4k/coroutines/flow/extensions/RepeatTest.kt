package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResultSet
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RepeatTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    private fun <T> Iterable<T>.cycled(): Sequence<T> {
        val self = this@cycled
        return sequence {
            while (true) {
                for (item in self) {
                    yield(item)
                }
            }
        }
    }

    @Nested
    inner class RepeatForever {

        @Test
        fun `never stop flow`() = runTest(timeout = 5.seconds) {
            val flow = flow<Int> { delay(1) }
                .log("#1")
                .repeat()

            val destination = mutableListOf<Int>()
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                flow.toList(destination)
            }
            val internalJob = intervalFlowOf(Duration.ZERO, 100.milliseconds)
                .log("#2")
                .take(1_000)
                .launchIn(this)

            runCurrent()

            repeat(100) {
                advanceTimeBy(10)
                runCurrent()
                destination.isEmpty().shouldBeTrue()
            }

            internalJob.cancelAndJoin()
            job.cancelAndJoin()

            destination.isEmpty().shouldBeTrue()
        }

        @Test
        fun `repeat elements`() = runTest {
            flowOf(1, 2, 3)
                .log("#1")
                .repeat()
                .take(100)
                .assertResultSet(listOf(1, 2, 3).cycled().take(100).toList())
        }

        @Test
        fun `repeat with fixed delay`() = runTest {
            var job: Job? = null
            val delay = 500.milliseconds

            flow {
                job.shouldBeNull()

                emit(1)
                emit(2)
                emit(3)

                job.shouldBeNull()

                job = launch {
                    delay(delay * 0.99)
                    job = null
                }
            }
                .log("#1")
                .repeat(delay)
                .take(100)
                .assertResultSet(listOf(1, 2, 3).cycled().take(100).toList())
        }

        @Test
        fun `repeat with delay function`() = runTest {
            var job: Job? = null
            var count = 0

            flow {
                job.shouldBeNull()

                emit(1)
                emit(2)
                emit(3)

                job.shouldBeNull()

                job = launch {
                    delay((count++).milliseconds * 0.99)
                    job = null
                }
            }
                .log("#1")
                .repeat { (it + 1).milliseconds }
                .take(100)
                .assertResultSet(listOf(1, 2, 3).cycled().take(100).toList())
        }

        @Test
        fun `repeat with exception`() = runTest {
            val flow = flow {
                emit(1)
                throw RuntimeException("Boom!")
            }

            assertFailsWith<RuntimeException> {
                flow.log("#1").repeat().collect()
            }
        }
    }

    @Nested
    inner class RepeatAtMost {

        @Test
        fun `repeat with zero count`() = runTest {
            flowOf(1, 2, 3)
                .log("#1")
                .repeat(0)
                .count() shouldBeEqualTo 0
        }

        @Test
        fun `repeat with negative count`() = runTest {
            flowOf(1, 2, 3)
                .log("#1")
                .repeat(-1)
                .count() shouldBeEqualTo 0
        }

        @Test
        fun `repeat with count`() = runTest {
            flowOf(1, 2, 3)
                .log("#1")
                .repeat(100)
                .assertResultSet(listOf(1, 2, 3).cycled().take(300).toList())
        }

        @Test
        fun `repeat with count and fixed delay`() = runTest {
            var job: Job? = null
            val delay = 500.milliseconds

            flow {
                job.shouldBeNull()

                emit(1)
                emit(2)
                emit(3)

                job.shouldBeNull()

                job = launch {
                    delay(delay * 0.99)
                    job = null
                }
            }
                .log("#1")
                .repeat(100, delay)
                .assertResultSet(listOf(1, 2, 3).cycled().take(300).toList())
        }

        @Test
        fun `repeat with count and delay function`() = runTest {
            var job: Job? = null
            var count = 0

            flow {
                job.shouldBeNull()

                emit(1)
                emit(2)
                emit(3)

                job.shouldBeNull()

                job = launch {
                    delay((count++).milliseconds * 0.99)
                    job = null
                }
            }
                .log("#1")
                .repeat(100) { (it + 1).milliseconds }
                .assertResultSet(listOf(1, 2, 3).cycled().take(300).toList())
        }

        @Test
        fun `repeat with count has exception`() = runTest {
            val flow = flow {
                emit(1)
                throw RuntimeException("Boom!")
            }

            assertFailsWith<RuntimeException> {
                flow.log("#1").repeat(100).collect()
            }
        }
    }
}

package io.bluetape4k.io.okio

import io.bluetape4k.junit5.concurrency.TestingExecutors
import io.bluetape4k.logging.KLogging
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import org.junit.jupiter.api.Timeout as JUnitTimeout

@JUnitTimeout(5, unit = TimeUnit.SECONDS)
class TimeoutTest: AbstractOkioTest() {

    companion object: KLogging() {
        val smallerTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(500L)
        val biggerTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(1500L)

        val smallerDeadLineNanos = TimeUnit.MILLISECONDS.toNanos(500L)
        val biggerDeadLineNanos = TimeUnit.MILLISECONDS.toNanos(1500L)
    }

    private lateinit var executor: ExecutorService

    @BeforeEach
    fun beforeEach() {
        executor = TestingExecutors.newFixedThreadPool(1)
    }

    @AfterEach
    fun afterEach() {
        runCatching {
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `intersect with returns a value`() {
        val timeoutA = Timeout()
        val timeoutB = Timeout()

        val s = timeoutA.intersectWith(timeoutB) { "hello" }
        s shouldBeEqualTo "hello"
    }

    @Test
    fun `intersect with prefers smaller timeout`() {
        val timeoutA = Timeout()
        timeoutA.timeout(smallerTimeoutNanos, TimeUnit.NANOSECONDS)

        val timeoutB = Timeout()
        timeoutB.timeout(biggerTimeoutNanos, TimeUnit.NANOSECONDS)

        timeoutA.intersectWith(timeoutB) {
            timeoutA.timeoutNanos() shouldBeEqualTo smallerTimeoutNanos
            timeoutB.timeoutNanos() shouldBeEqualTo biggerTimeoutNanos
        }

        timeoutB.intersectWith(timeoutA) {
            timeoutA.timeoutNanos() shouldBeEqualTo smallerTimeoutNanos
            timeoutB.timeoutNanos() shouldBeEqualTo smallerTimeoutNanos
        }

        timeoutA.timeoutNanos() shouldBeEqualTo smallerTimeoutNanos
        timeoutB.timeoutNanos() shouldBeEqualTo biggerTimeoutNanos
    }
}

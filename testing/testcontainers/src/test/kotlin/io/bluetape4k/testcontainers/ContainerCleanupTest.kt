package io.bluetape4k.testcontainers

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

class ContainerCleanupTest {

    @Test
    fun `cleanup timeout interrupts the pending operation`() {
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)

        assertFailsWith<TimeoutException> {
            runCleanupWithin(100.milliseconds) {
                started.countDown()
                try {
                    Thread.sleep(5.seconds.inWholeMilliseconds)
                } catch (e: InterruptedException) {
                    interrupted.countDown()
                    throw e
                }
            }
        }

        started.await(1, TimeUnit.SECONDS).shouldBeTrue()
        interrupted.await(1, TimeUnit.SECONDS).shouldBeTrue()
    }

    @Test
    fun `cleanup failure is propagated`() {
        assertFailsWith<IllegalStateException> {
            runCleanupWithin(1.seconds) {
                error("cleanup failed")
            }
        }
    }
}

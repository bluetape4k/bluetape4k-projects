package io.bluetape4k.io.okio

import io.bluetape4k.junit5.concurrency.TestingExecutors
import io.bluetape4k.junit5.system.assumeNotWindows
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.notify
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.InterruptedIOException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.test.fail

class WaitUntilNotifiedTest: AbstractOkioTest() {

    companion object: KLogging()

    private lateinit var executor: ScheduledExecutorService

    @BeforeEach
    fun beforeEach() {
        executor = TestingExecutors.newScheduledExecutorService(1)
    }

    @AfterEach
    fun afterEach() {
        executor.shutdown()
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `notified with timeout`(factory: TimeoutFactory) {
        val timeout = factory.newTimeout()
        timeout.timeout(5000, TimeUnit.MILLISECONDS)
        val start = now()
        executor.schedule(
            {
                synchronized(this) {
                    this.notify()
                }
            },
            1000,
            TimeUnit.MILLISECONDS
        )

        timeout.waitUntilNotified(this)
        assertElapsed(1000.0, start)
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `wait until notified`(factory: TimeoutFactory) {
        assumeNotWindows()
        val timeout = factory.newTimeout()
        timeout.timeout(1000, TimeUnit.MILLISECONDS)
        val start = now()

        try {
            // 1초 후에는 InterruptedIOException이 발생해야 한다.
            timeout.waitUntilNotified(this)
            fail("Should not reach here")
        } catch (expected: InterruptedIOException) {
            expected.message shouldBeEqualTo "timeout"
        }
        assertElapsed(1000.0, start)
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `deadline only`(factory: TimeoutFactory) {
        assumeNotWindows()
        val timeout = factory.newTimeout()
        timeout.deadline(1000, TimeUnit.MILLISECONDS)
        val start = now()

        try {
            // 1초 후에는 InterruptedIOException이 발생해야 한다.
            timeout.waitUntilNotified(this)
            fail("Should not reach here")
        } catch (expected: InterruptedIOException) {
            expected.message shouldBeEqualTo "timeout"
        }
        assertElapsed(1000.0, start)
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `deadline before timeout`(factory: TimeoutFactory) {
        assumeNotWindows()
        val timeout = factory.newTimeout()
        timeout.timeout(5000, TimeUnit.SECONDS)
        timeout.deadline(1000, TimeUnit.MILLISECONDS)
        val start = now()

        try {
            // 1초 후에는 InterruptedIOException이 발생해야 한다.
            timeout.waitUntilNotified(this)
            fail("Should not reach here")
        } catch (expected: InterruptedIOException) {
            expected.message shouldBeEqualTo "timeout"
        }
        assertElapsed(1000.0, start)
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `deadline already reached`(factory: TimeoutFactory) {
        assumeNotWindows()
        val timeout = factory.newTimeout()
        timeout.deadlineNanoTime(System.nanoTime())
        val start = now()

        try {
            // 1초 후에는 InterruptedIOException이 발생해야 한다.
            timeout.waitUntilNotified(this)
            fail("Should not reach here")
        } catch (expected: InterruptedIOException) {
            expected.message shouldBeEqualTo "timeout"
        }
        assertElapsed(0.0, start)
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `thread interrupted`(factory: TimeoutFactory) {
        assumeNotWindows()
        val timeout = factory.newTimeout()
        val start = now()

        Thread.currentThread().interrupt()

        try {
            // 1초 후에는 InterruptedIOException이 발생해야 한다.
            timeout.waitUntilNotified(this)
            fail("Should not reach here")
        } catch (expected: InterruptedIOException) {
            expected.message shouldBeEqualTo "interrupted"
            Thread.interrupted().shouldBeTrue()
        }
        assertElapsed(0.0, start)
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `thread interrupted on throw if reached`(factory: TimeoutFactory) {
        assumeNotWindows()
        val timeout = factory.newTimeout()

        Thread.currentThread().interrupt()

        try {
            // 1초 후에는 InterruptedIOException이 발생해야 한다.
            timeout.throwIfReached()
            fail("Should not reach here")
        } catch (expected: InterruptedIOException) {
            expected.message shouldBeEqualTo "interrupted"
            Thread.interrupted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `cancel before wait does nothing`(factory: TimeoutFactory) {
        assumeNotWindows()
        val timeout = factory.newTimeout()
        timeout.timeout(1000, TimeUnit.MILLISECONDS)
        timeout.cancel()  // 모든 걸 취소한다
        val start = now()

        try {
            // 1초 후에는 InterruptedIOException이 발생해야 한다.
            timeout.waitUntilNotified(this)
            fail("Should not reach here")
        } catch (expected: InterruptedIOException) {
            expected.message shouldBeEqualTo "timeout"
        }
        assertElapsed(1000.0, start)
    }

    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `canceled timeout does not throw when not notified on time`(factory: TimeoutFactory) {
        val timeout = factory.newTimeout()
        timeout.timeout(1000, TimeUnit.MILLISECONDS)
        timeout.cancelLater(500)   // 취소를 수행하면 timeout 이 발생해도 예외가 발생하지 않는다 (cancel 이 먼저 수행되었기 때문)

        val start = now()
        timeout.waitUntilNotified(this)  // Returns early but doesn't throw.
        assertElapsed(1000.0, start)
    }


    @ParameterizedTest
    @EnumSource(TimeoutFactory::class)
    @Synchronized
    fun `multiple cancels are idempotent`(factory: TimeoutFactory) {
        val timeout = factory.newTimeout()
        timeout.timeout(1000, TimeUnit.MILLISECONDS)

        timeout.cancelLater(250)
        timeout.cancelLater(500)   // 취소를 수행하면 timeout 이 발생해도 예외가 발생하지 않는다 (cancel 이 먼저 수행되었기 때문)
        timeout.cancelLater(750)

        val start = now()
        timeout.waitUntilNotified(this)  // Returns early but doesn't throw.
        assertElapsed(1000.0, start)
    }

    private fun Timeout.cancelLater(delay: Long) {
        executor.schedule({ cancel() }, delay, TimeUnit.MILLISECONDS)
    }
}

package io.bluetape4k.okio.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.okio.AbstractOkioTest
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import org.junit.jupiter.api.Test
import java.io.InterruptedIOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BlockingInteropTimeoutTest: AbstractOkioTest() {

    @Test
    fun `asBlocking source read timeout은 예외를 던진다`() {
        val suspended = object: SuspendedSource {
            override suspend fun read(sink: Buffer, byteCount: Long): Long {
                delay(100.milliseconds)
                return -1L
            }

            override suspend fun close() {}

            override fun timeout(): Timeout = Timeout().timeout(1, TimeUnit.MILLISECONDS)
        }

        val source = suspended.asBlocking()
        assertFailsWith<InterruptedIOException> {
            source.read(Buffer(), 1L)
        }
    }

    @Test
    fun `asBlocking sink write timeout은 예외를 던진다`() {
        val suspended = object: SuspendedSink {
            override suspend fun write(source: Buffer, byteCount: Long) {
                delay(100.milliseconds)
            }

            override suspend fun flush() {}
            override suspend fun close() {}

            override fun timeout(): Timeout = Timeout().timeout(1, TimeUnit.MILLISECONDS)
        }

        val sink = suspended.asBlocking()
        assertFailsWith<InterruptedIOException> {
            sink.write(Buffer().writeUtf8("timeout"), 7L)
        }
    }

    @Test
    fun `asSuspended source read cancellation interrupts blocking delegate`() = runSuspendIO {
        val started = CountDownLatch(1)
        val interrupted = AtomicBoolean(false)
        val suspended = object: Source {
            override fun read(sink: Buffer, byteCount: Long): Long {
                started.countDown()
                try {
                    Thread.sleep(10_000L)
                    return -1L
                } catch (e: InterruptedException) {
                    interrupted.set(true)
                    throw InterruptedIOException("interrupted")
                }
            }

            override fun close() {}
            override fun timeout(): Timeout = Timeout.NONE
        }.asSuspended()

        supervisorScope {
            val job = async {
                suspended.read(Buffer(), 1L)
            }

            started.await(1, TimeUnit.SECONDS).shouldBeTrue()
            withTimeout(1.seconds) {
                job.cancelAndJoin()
            }
        }

        interrupted.get().shouldBeTrue()
    }

    @Test
    fun `asSuspended sink write cancellation interrupts blocking delegate`() = runSuspendIO {
        val started = CountDownLatch(1)
        val interrupted = AtomicBoolean(false)
        val suspended = object: Sink {
            override fun write(source: Buffer, byteCount: Long) {
                started.countDown()
                try {
                    Thread.sleep(10_000L)
                } catch (e: InterruptedException) {
                    interrupted.set(true)
                    throw InterruptedIOException("interrupted")
                }
            }

            override fun flush() {}
            override fun close() {}
            override fun timeout(): Timeout = Timeout.NONE
        }.asSuspended()

        supervisorScope {
            val job = async {
                suspended.write(Buffer().writeUtf8("cancel"), 6L)
            }

            started.await(1, TimeUnit.SECONDS).shouldBeTrue()
            withTimeout(1.seconds) {
                job.cancelAndJoin()
            }
        }

        interrupted.get().shouldBeTrue()
    }
}

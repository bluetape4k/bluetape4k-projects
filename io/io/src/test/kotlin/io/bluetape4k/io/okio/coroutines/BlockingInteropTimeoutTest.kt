package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import kotlinx.coroutines.delay
import okio.Buffer
import okio.Timeout
import org.junit.jupiter.api.Test
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

class BlockingInteropTimeoutTest: AbstractOkioTest() {

    @Test
    fun `asBlocking source read timeout은 예외를 던진다`() {
        val suspended = object: SuspendedSource {
            override suspend fun read(sink: Buffer, byteCount: Long): Long {
                delay(100)
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
                delay(100)
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
}

package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import net.datafaker.Faker
import okio.Buffer
import okio.IOException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SuspendedPipeTest: AbstractOkioTest() {

    companion object: KLoggingChannel() {
        private const val MAX_BUFFER_SIZE = 1024L
    }

    @Test
    fun `write and read basic`() = runSuspendIO {
        val message = Faker().lorem().paragraph(10)
        val messageLength = message.length.toLong()
        val pipe = SuspendedPipe(messageLength * 2L)
        val writeBuffer = bufferOf(message)
        pipe.sink.write(writeBuffer, writeBuffer.size)
        pipe.sink.close()

        val readBuffer = Buffer()

        val bytesRead = pipe.source.read(readBuffer, messageLength * 2L)
        bytesRead shouldBeEqualTo messageLength
        readBuffer.readUtf8() shouldBeEqualTo message
        pipe.source.read(readBuffer, 1024) shouldBeEqualTo -1L
    }

    @Test
    fun `cancel pipe fails operations`() = runSuspendIO {
        val pipe = SuspendedPipe(1024)
        pipe.cancel()
        val writeBuffer = bufferOf("fail")

        assertFailsWith<IOException> {
            pipe.sink.write(writeBuffer, writeBuffer.size)
        }

        assertFailsWith<IOException> {
            pipe.source.read(Buffer(), 10)
        }
    }

    @Test
    fun `read with zero byteCount returns immediately`() = runSuspendIO {
        val pipe = SuspendedPipe(MAX_BUFFER_SIZE)
        pipe.source.read(Buffer(), 0L) shouldBeEqualTo 0L
    }

    @Test
    fun `pipe ignores non-positive byteCount and validates write upper range`() = runSuspendIO {
        val pipe = SuspendedPipe(MAX_BUFFER_SIZE)
        val source = bufferOf("hello")

        pipe.sink.write(source.copy(), -1L)
        pipe.sink.write(source.copy(), 0L)
        assertFailsWith<IllegalArgumentException> {
            pipe.source.read(Buffer(), -1L)
        }

        assertFailsWith<IllegalArgumentException> {
            pipe.sink.write(source.copy(), source.size + 1L)
        }
    }

    @Test
    fun `fold transfers buffer and closes source`() = runSuspendIO {
        val pipe = SuspendedPipe(1024)
        val writeBuffer = bufferOf("folded")
        pipe.sink.write(writeBuffer, writeBuffer.size)

        val foldedSink = object: SuspendedSink {
            val result = Buffer()
            override suspend fun write(source: Buffer, byteCount: Long) {
                result.write(source, byteCount)
            }

            override suspend fun flush() {}
            override suspend fun close() {}
            override fun timeout() = pipe.sink.timeout()
        }
        pipe.fold(foldedSink)
        foldedSink.result.readUtf8() shouldBeEqualTo "folded"

        assertFailsWith<IllegalStateException> {
            pipe.source.read(Buffer(), 10)
        }
    }
}

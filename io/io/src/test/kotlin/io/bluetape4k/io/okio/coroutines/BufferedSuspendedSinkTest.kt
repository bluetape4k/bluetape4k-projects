package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith

class BufferedSuspendedSinkTest: AbstractOkioTest() {

    companion object: KLoggingChannel()

    // 테스트용 FakeSuspendedSink
    private class FakeSuspendedSink: SuspendedSink {
        companion object: KLoggingChannel()

        val buffer = Buffer()
        var closed = false
            private set

        override suspend fun write(source: Buffer, byteCount: Long) {
            log.debug { "Read source and write to buffer. byteCount=$byteCount" }
            buffer.write(source, byteCount)
        }

        override suspend fun flush() {}
        override suspend fun close() {
            closed = true
        }

        override fun timeout() = Timeout.NONE
    }

    @Test
    fun `write and emitCompleteSegments writes only complete segments`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink: BufferedSuspendedSink = fakeSink.buffered() // RealBufferedSuspendedSink(fakeSink)
        val data = ByteArray(SEGMENT_SIZE.toInt()) { it.toByte() } // SEGMENT_SIZE * 2

        bufferedSink.write(data, 0, SEGMENT_SIZE.toInt())
        fakeSink.buffer.size shouldBeEqualTo SEGMENT_SIZE
    }

    @Test
    fun `flush writes remaining data`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        val text = "hello, world! 안녕하세요."
        val data = text.toByteArray()

        bufferedSink.write(data, 0, data.size)
        bufferedSink.flush()
        fakeSink.buffer.readUtf8() shouldBeEqualTo text
    }

    @Test
    fun `close flushes and closes underlying sink`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)

        bufferedSink.writeUtf8("bye", 0, 3)
        bufferedSink.close()
        fakeSink.closed shouldBeEqualTo true
        fakeSink.buffer.readUtf8() shouldBeEqualTo "bye"
    }

    @Test
    fun `writeInt and writeLong writes integer and long values`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        with(bufferedSink) {
            writeInt(0x12345678)
            writeLong(0x1122334455667788L)
            flush()
        }

        // int: 0x12 0x34 0x56 0x78, long: 0x11 0x22 0x33 0x44 0x55 0x66 0x77 0x88
        val expected = byteArrayOf(
            0x12, 0x34, 0x56, 0x78,
            0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte()
        )
        fakeSink.buffer.readByteArray() shouldBeEqualTo expected
    }

    @Test
    fun `write after close throws`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        bufferedSink.close()

        assertFailsWith<IllegalStateException> {
            bufferedSink.writeUtf8("fail", 0, 4)
        }
    }

    @Test
    fun `write from suspended source throws when no progress repeats`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        val noProgressSource = object: SuspendedSource {
            override suspend fun read(sink: Buffer, byteCount: Long): Long = 0L
            override suspend fun close() {}
            override fun timeout() = Timeout.NONE
        }

        assertFailsWith<IOException> {
            bufferedSink.write(noProgressSource, 1L)
        }
    }

    @Test
    fun `writeAll from suspended source throws when no progress repeats`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        val noProgressSource = object: SuspendedSource {
            override suspend fun read(sink: Buffer, byteCount: Long): Long = 0L
            override suspend fun close() {}
            override fun timeout() = Timeout.NONE
        }

        assertFailsWith<IOException> {
            bufferedSink.writeAll(noProgressSource)
        }
    }
}

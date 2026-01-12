package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BufferedSuspendedSinkTest: AbstractOkioTest() {

    companion object: KLoggingChannel()

    @Test
    fun `write and emitCompleteSegments writes only complete segments`() = runSuspendIO {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        val data = ByteArray(8192) { it.toByte() } // SEGMENT_SIZE * 2

        bufferedSink.write(data, 0, SEGMENT_SIZE.toInt())
        fakeSink.buffer.size shouldBeEqualTo SEGMENT_SIZE
    }

    @Test
    fun `flush writes remaining data`() = runSuspendIO {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        val data = "hello, world!".toByteArray()

        bufferedSink.write(data, 0, data.size)
        bufferedSink.flush()
        fakeSink.buffer.readUtf8() shouldBeEqualTo "hello, world!"
    }

    @Test
    fun `close flushes and closes underlying sink`() = runSuspendIO {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)

        bufferedSink.writeUtf8("bye", 0, 3)
        bufferedSink.close()
        fakeSink.closed shouldBeEqualTo true
        fakeSink.buffer.readUtf8() shouldBeEqualTo "bye"
    }

    @Test
    fun `writeInt and writeLong writes integer and long values`() = runSuspendIO {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)

        bufferedSink.writeInt(0x12345678)
        bufferedSink.writeLong(0x1122334455667788L)
        bufferedSink.flush()

        // int: 0x12 0x34 0x56 0x78, long: 0x11 0x22 0x33 0x44 0x55 0x66 0x77 0x88
        val expected = byteArrayOf(
            0x12, 0x34, 0x56, 0x78,
            0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte()
        )
        fakeSink.buffer.readByteArray() shouldBeEqualTo expected
    }

    @Test
    fun `write after close throws`() = runSuspendIO {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        bufferedSink.close()

        assertFailsWith<IllegalStateException> {
            bufferedSink.writeUtf8("fail", 0, 4)
        }
    }

    // 테스트용 FakeSuspendedSink
    private class FakeSuspendedSink: SuspendedSink {
        val buffer = Buffer()
        var closed = false
        override suspend fun write(source: Buffer, byteCount: Long) {
            buffer.write(source, byteCount)
        }

        override suspend fun flush() {}
        override suspend fun close() {
            closed = true
        }

        override fun timeout() = Timeout.NONE
    }
}

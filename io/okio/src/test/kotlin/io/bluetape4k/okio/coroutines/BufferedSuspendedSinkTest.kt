package io.bluetape4k.okio.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.okio.AbstractOkioTest
import io.bluetape4k.okio.SEGMENT_SIZE
import io.bluetape4k.okio.bufferOf
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Timeout
import org.junit.jupiter.api.Test
import java.io.IOException

class BufferedSuspendedSinkTest: AbstractOkioTest() {

    companion object: KLoggingChannel()

    // 테스트용 FakeSuspendedSink
    private class FakeSuspendedSink: SuspendedSink {
        companion object: KLoggingChannel()

        val buffer = Buffer()
        var closed = false
            private set
        var flushCount = 0
            private set
        var closeCount = 0
            private set

        override suspend fun write(source: Buffer, byteCount: Long) {
            log.debug { "Read source and write to buffer. byteCount=$byteCount" }
            buffer.write(source, byteCount)
        }

        override suspend fun flush() {
            flushCount++
        }
        override suspend fun close() {
            closeCount++
            closed = true
        }

        override fun timeout() = Timeout.NONE
    }

    @Test
    fun `emitCompleteSegments keeps complete segments delegated and tail internal`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink: BufferedSuspendedSink = fakeSink.buffered()
        val completeSegment = ByteArray(SEGMENT_SIZE.toInt()) { it.toByte() }
        val tail = "tail-after-complete-segment".encodeUtf8()
        val expectedDelegate = Buffer().apply { write(completeSegment) }.snapshot()

        bufferedSink.write(completeSegment, 0, completeSegment.size)
        fakeSink.buffer.snapshot() shouldBeEqualTo expectedDelegate
        bufferedSink.buffer.snapshot() shouldBeEqualTo ByteString.EMPTY

        bufferedSink.write(tail)
        fakeSink.buffer.snapshot() shouldBeEqualTo expectedDelegate
        bufferedSink.buffer.snapshot() shouldBeEqualTo tail
        fakeSink.flushCount shouldBeEqualTo 0
        fakeSink.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `flush transfers one tail and close transfers subsequent tail exactly once`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        val firstTail = "tail-before-flush".encodeUtf8()
        val secondTail = "tail-before-close".encodeUtf8()
        val expected = Buffer().apply {
            write(firstTail)
            write(secondTail)
        }.snapshot()

        bufferedSink.write(firstTail)
        fakeSink.buffer.snapshot() shouldBeEqualTo ByteString.EMPTY
        bufferedSink.buffer.snapshot() shouldBeEqualTo firstTail

        bufferedSink.flush()
        fakeSink.buffer.snapshot() shouldBeEqualTo firstTail
        bufferedSink.buffer.snapshot() shouldBeEqualTo ByteString.EMPTY
        fakeSink.flushCount shouldBeEqualTo 1

        bufferedSink.write(secondTail)
        fakeSink.buffer.snapshot() shouldBeEqualTo firstTail
        bufferedSink.buffer.snapshot() shouldBeEqualTo secondTail

        bufferedSink.close()
        fakeSink.buffer.snapshot() shouldBeEqualTo expected
        bufferedSink.buffer.snapshot() shouldBeEqualTo ByteString.EMPTY
        fakeSink.flushCount shouldBeEqualTo 1
        fakeSink.closeCount shouldBeEqualTo 1
    }

    @Test
    fun `close flushes and closes underlying sink`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)

        bufferedSink.writeUtf8("bye", 0, 3)
        bufferedSink.close()
        fakeSink.closed.shouldBeTrue()
        fakeSink.buffer.snapshot() shouldBeEqualTo "bye".encodeUtf8()
        fakeSink.closeCount shouldBeEqualTo 1
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

        val expected = Buffer().apply {
            writeInt(0x12345678)
            writeLong(0x1122334455667788L)
        }.snapshot()
        fakeSink.buffer.snapshot() shouldBeEqualTo expected
        fakeSink.flushCount shouldBeEqualTo 1
    }

    @Test
    fun `all buffered write overloads preserve exact payload`() = runTest {
        val fakeSink = FakeSuspendedSink()
        val bufferedSink = RealBufferedSuspendedSink(fakeSink)
        val expected = Buffer()
        val writeAllPayload = "write-all-sentinel"

        bufferedSink.writeByteStringAndTextOverloads(expected)
        bufferedSink.writeNumericOverloads(expected)
        val writeAllCount = bufferedSink.writeSourceOverloads(expected, writeAllPayload)
        writeAllCount shouldBeEqualTo writeAllPayload.encodeUtf8().size.toLong()

        bufferedSink.emitCompleteSegments()
        bufferedSink.emit()
        bufferedSink.flush()

        fakeSink.buffer.snapshot() shouldBeEqualTo expected.snapshot()
        bufferedSink.buffer.snapshot() shouldBeEqualTo ByteString.EMPTY
        fakeSink.flushCount shouldBeEqualTo 1
    }

    private suspend fun BufferedSuspendedSink.writeByteStringAndTextOverloads(expected: Buffer) {
        val byteString = "byte-string-sentinel".encodeUtf8()
        write(byteString)
        expected.write(byteString)

        val byteArray = byteArrayOf(0x21, 0x22, 0x23, 0x24, 0x25)
        val byteArrayOffset = 1
        val byteArrayCount = 3
        write(byteArray, byteArrayOffset, byteArrayCount)
        expected.write(byteArray, byteArrayOffset, byteArrayCount)

        val fullUtf8 = "utf8-full-sentinel-한글"
        writeUtf8(fullUtf8)
        expected.writeUtf8(fullUtf8)

        val utf8 = "prefix-utf8-range-suffix"
        val utf8BeginIndex = 7
        val utf8EndIndex = 17
        writeUtf8(utf8, utf8BeginIndex, utf8EndIndex)
        expected.writeUtf8(utf8, utf8BeginIndex, utf8EndIndex)

        val codePoint = 0x1F642
        writeUtf8CodePoint(codePoint)
        expected.writeUtf8CodePoint(codePoint)
    }

    private suspend fun BufferedSuspendedSink.writeNumericOverloads(expected: Buffer) {
        val byte = 0x2A
        writeByte(byte)
        expected.writeByte(byte)

        val short = 0x3142
        writeShort(short)
        expected.writeShort(short)

        val shortLe = 0x4354
        writeShortLe(shortLe)
        expected.writeShortLe(shortLe)

        val int = 0x51525354
        writeInt(int)
        expected.writeInt(int)

        val intLe = 0x61626364
        writeIntLe(intLe)
        expected.writeIntLe(intLe)

        val long = 0x0102030405060708L
        writeLong(long)
        expected.writeLong(long)

        val longLe = 0x1112131415161718L
        writeLongLe(longLe)
        expected.writeLongLe(longLe)

        val decimalLong = -9876543210L
        writeDecimalLong(decimalLong)
        expected.writeDecimalLong(decimalLong)

        val hexadecimalUnsignedLong = 0x1234ABCDL
        writeHexadecimalUnsignedLong(hexadecimalUnsignedLong)
        expected.writeHexadecimalUnsignedLong(hexadecimalUnsignedLong)
    }

    private suspend fun BufferedSuspendedSink.writeSourceOverloads(expected: Buffer, writeAllPayload: String): Long {
        val bufferPayload = "buffer-source-sentinel"
        val bufferByteCount = 6L
        val bufferSource = bufferOf(bufferPayload)
        val expectedBufferSource = bufferOf(bufferPayload)
        write(bufferSource, bufferByteCount)
        expected.write(expectedBufferSource, bufferByteCount)

        val fixedSourcePayload = "fixed-source-sentinel"
        val fixedByteCount = 7L
        val fixedSource: okio.Source = bufferOf(fixedSourcePayload)
        val expectedFixedSource = bufferOf(fixedSourcePayload)
        write(fixedSource.asSuspended(), fixedByteCount)
        expected.write(expectedFixedSource, fixedByteCount)

        val writeAllSource: okio.Source = bufferOf(writeAllPayload)
        val expectedWriteAllSource = bufferOf(writeAllPayload)
        val writeAllCount = writeAll(writeAllSource.asSuspended())
        expected.write(expectedWriteAllSource, expectedWriteAllSource.size)
        return writeAllCount
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

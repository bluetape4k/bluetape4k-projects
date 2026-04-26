package io.bluetape4k.okio.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.okio.AbstractOkioTest
import okio.Buffer
import okio.ByteString
import okio.EOFException
import okio.Options
import okio.ByteString.Companion.encodeUtf8
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BufferedSuspendSourceTest: AbstractOkioTest() {

    companion object: KLoggingChannel()

    private class FakeSuspendedSource(private val data: Buffer): SuspendedSource {
        companion object: KLoggingChannel()

        var closed = false
            private set

        override suspend fun read(sink: Buffer, byteCount: Long): Long {
            log.debug { "데이터를 읽어 sink에 씁니다. buffer size=${data.size}, byteCount=$byteCount" }
            return data.read(sink, minOf(data.size, byteCount))
        }

        override suspend fun close() {
            closed = true
        }

        override fun timeout() = okio.Timeout.NONE
    }

    private class FakeSuspendedSink(private val buffer: Buffer): SuspendedSink {
        override suspend fun write(source: Buffer, byteCount: Long) {
            buffer.write(source, byteCount)
        }
        override suspend fun flush() {}
        override suspend fun close() {}
    }

    @Test
    fun `readByte returns correct byte`() = runSuspendIO {
        val buffer = Buffer().writeByte(0x7F)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readByte() shouldBeEqualTo 0x7F.toByte()
    }

    @Test
    fun `readShort returns correct short`() = runSuspendIO {
        val buffer = Buffer().writeShort(0x1234)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readShort() shouldBeEqualTo 0x1234.toShort()
    }

    @Test
    fun `readShortLe returns correct little-endian short`() = runSuspendIO {
        val buffer = Buffer().writeShortLe(0x1234)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readShortLe() shouldBeEqualTo 0x1234.toShort()
    }

    @Test
    fun `readInt returns correct int`() = runSuspendIO {
        val buffer = Buffer().writeInt(0x12345678)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readInt() shouldBeEqualTo 0x12345678
    }

    @Test
    fun `readIntLe returns correct little-endian int`() = runSuspendIO {
        val buffer = Buffer().writeIntLe(0x12345678)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readIntLe() shouldBeEqualTo 0x12345678
    }

    @Test
    fun `readLong returns correct long`() = runSuspendIO {
        val buffer = Buffer().writeLong(0x123456789ABCDEF0)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readLong() shouldBeEqualTo 0x123456789ABCDEF0L
    }

    @Test
    fun `readLongLe returns correct little-endian long`() = runSuspendIO {
        val buffer = Buffer().writeLongLe(0x123456789ABCDEF0L)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readLongLe() shouldBeEqualTo 0x123456789ABCDEF0L
    }

    @Test
    fun `readDecimalLong reads positive decimal number`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("12345 rest")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readDecimalLong() shouldBeEqualTo 12345L
    }

    @Test
    fun `readDecimalLong reads negative decimal number`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("-678 rest")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readDecimalLong() shouldBeEqualTo -678L
    }

    @Test
    fun `readHexadecimalUnsignedLong reads hex number`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("deadbeef rest")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readHexadecimalUnsignedLong() shouldBeEqualTo 0xDEADBEEFL
    }

    @Test
    fun `readHexadecimalUnsignedLong reads uppercase hex number`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("1A2B rest")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readHexadecimalUnsignedLong() shouldBeEqualTo 0x1A2BL
    }

    @Test
    fun `skip skips specified bytes`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.skip(6L)
        source.readUtf8() shouldBeEqualTo "world"
    }

    @Test
    fun `skip throws EOFException when not enough bytes`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hi")
        val source = FakeSuspendedSource(buffer).buffered()
        assertFailsWith<EOFException> { source.skip(10L) }
    }

    @Test
    fun `readByteString reads all bytes as ByteString`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readByteString() shouldBeEqualTo ByteString.of(*bytes)
    }

    @Test
    fun `readByteString with count reads correct bytes`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readByteString(3L) shouldBeEqualTo ByteString.of(1, 2, 3)
    }

    @Test
    fun `select returns matching option index`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("world")
        val source = FakeSuspendedSource(buffer).buffered()
        val options = Options.of(
            "hello".encodeUtf8(),
            "world".encodeUtf8()
        )
        source.select(options) shouldBeEqualTo 1
    }

    @Test
    fun `select returns -1 when no option matches`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("foo")
        val source = FakeSuspendedSource(buffer).buffered()
        val options = Options.of(
            "hello".encodeUtf8(),
            "world".encodeUtf8()
        )
        source.select(options) shouldBeEqualTo -1
    }

    @Test
    fun `readByteArray reads all bytes`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readByteArray() shouldBeEqualTo bytes
    }

    @Test
    fun `readByteArray with count reads correct bytes`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        source.readByteArray(3) shouldBeEqualTo byteArrayOf(1, 2, 3)
    }

    @Test
    fun `read into ByteArray returns read byte count`() = runSuspendIO {
        val bytes = byteArrayOf(10, 20, 30, 40)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = ByteArray(4)
        val read = source.read(sink)
        read shouldBeEqualTo 4
        sink shouldBeEqualTo bytes
    }

    @Test
    fun `read into ByteArray with offset and count`() = runSuspendIO {
        val bytes = byteArrayOf(10, 20, 30, 40)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = ByteArray(6)
        val read = source.read(sink, 1, 4)
        read shouldBeEqualTo 4
        sink.copyOfRange(1, 5) shouldBeEqualTo bytes
    }

    @Test
    fun `read into ByteArray returns -1 when exhausted`() = runSuspendIO {
        val buffer = Buffer()
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = ByteArray(4)
        source.read(sink) shouldBeEqualTo -1
    }

    @Test
    fun `read into Buffer returns read byte count`() = runSuspendIO {
        val bytes = byteArrayOf(10, 20, 30)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = Buffer()
        val read = source.read(sink, 3L)
        read shouldBeEqualTo 3L
        sink.readByteArray() shouldBeEqualTo bytes
    }

    @Test
    fun `read into Buffer returns -1 when exhausted`() = runSuspendIO {
        val buffer = Buffer()
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = Buffer()
        source.read(sink, 1L) shouldBeEqualTo -1L
    }

    @Test
    fun `readFully fills ByteArray completely`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = ByteArray(4)
        source.readFully(sink)
        sink shouldBeEqualTo bytes
    }

    @Test
    fun `readFully ByteArray throws EOFException when not enough bytes`() = runSuspendIO {
        val buffer = Buffer().writeByte(1)
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = ByteArray(4)
        assertFailsWith<EOFException> { source.readFully(sink) }
    }

    @Test
    fun `readFully Buffer fills required bytes`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = Buffer()
        source.readFully(sink, 3L)
        sink.size shouldBeEqualTo 3L
        sink.readByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3)
    }

    @Test
    fun `readFully Buffer throws EOFException when not enough bytes`() = runSuspendIO {
        val buffer = Buffer().writeByte(1)
        val source = FakeSuspendedSource(buffer).buffered()
        val sink = Buffer()
        assertFailsWith<EOFException> { source.readFully(sink, 4L) }
    }

    @Test
    fun `readAll reads all bytes to sink and returns total`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = Buffer().write(bytes)
        val source = FakeSuspendedSource(buffer).buffered()
        val sinkBuffer = Buffer()
        val total = source.readAll(FakeSuspendedSink(sinkBuffer))
        total shouldBeEqualTo 5L
        sinkBuffer.readByteArray() shouldBeEqualTo bytes
    }

    @Test
    fun `readAll handles data spanning multiple segments`() = runSuspendIO {
        val largeBytes = ByteArray(16_384) { (it % 256).toByte() }
        val buffer = Buffer().write(largeBytes)
        val source = FakeSuspendedSource(buffer).buffered()
        val sinkBuffer = Buffer()
        val total = source.readAll(FakeSuspendedSink(sinkBuffer))
        total shouldBeEqualTo 16_384L
        sinkBuffer.size shouldBeEqualTo 16_384L
    }

    @Test
    fun `readUtf8 reads string`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `readUtf8 with byteCount reads partial string`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readUtf8(5L) shouldBeEqualTo "hello"
    }

    @Test
    fun `readUtf8Line reads LF-terminated line`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello\nworld")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readUtf8Line() shouldBeEqualTo "hello"
        source.readUtf8Line() shouldBeEqualTo "world"
        source.readUtf8Line().shouldBeNull()
    }

    @Test
    fun `readUtf8Line handles CRLF line endings`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello\r\nworld\r\n")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readUtf8Line() shouldBeEqualTo "hello"
        source.readUtf8Line() shouldBeEqualTo "world"
        source.readUtf8Line().shouldBeNull()
    }

    @Test
    fun `readUtf8LineStrict reads line with newline`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello\nworld")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readUtf8LineStrict() shouldBeEqualTo "hello"
    }

    @Test
    fun `readUtf8LineStrict throws EOFException when no newline`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("no newline here")
        val source = FakeSuspendedSource(buffer).buffered()
        assertFailsWith<EOFException> { source.readUtf8LineStrict() }
    }

    @Test
    fun `readUtf8LineStrict with limit throws EOFException when line exceeds limit`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("toolongline\n")
        val source = FakeSuspendedSource(buffer).buffered()
        assertFailsWith<EOFException> { source.readUtf8LineStrict(5L) }
    }

    @Test
    fun `readUtf8CodePoint reads ASCII character`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("A")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readUtf8CodePoint() shouldBeEqualTo 'A'.code
    }

    @Test
    fun `readUtf8CodePoint reads multibyte UTF-8 character`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("한")
        val source = FakeSuspendedSource(buffer).buffered()
        source.readUtf8CodePoint() shouldBeEqualTo '한'.code
    }

    @Test
    fun `exhausted returns true when source is empty`() = runSuspendIO {
        val buffer = Buffer()
        val source = FakeSuspendedSource(buffer).buffered()
        source.exhausted().shouldBeTrue()
    }

    @Test
    fun `require throws EOFException if not enough bytes`() = runSuspendIO {
        val buffer = Buffer().writeByte(1)
        val source = FakeSuspendedSource(buffer).buffered()
        assertFailsWith<EOFException> { source.require(2) }
    }

    @Test
    fun `indexOf finds byte in specified range`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.indexOf('o'.code.toByte(), 0L, 11L) shouldBeEqualTo 4L
    }

    @Test
    fun `indexOf returns -1 when byte not in range`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.indexOf('o'.code.toByte(), 0L, 3L) shouldBeEqualTo -1L
    }

    @Test
    fun `indexOf ByteString finds substring`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.indexOf("world".encodeUtf8(), 0L) shouldBeEqualTo 6L
    }

    @Test
    fun `indexOf ByteString returns -1 when not found`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.indexOf("xyz".encodeUtf8(), 0L) shouldBeEqualTo -1L
    }

    @Test
    fun `indexOfElement finds substring sequence within toIndex range`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        // indexOfElement delegates to indexOf(ByteString) — finds the complete byte sequence
        source.indexOfElement("ell".encodeUtf8(), 0L, 10L) shouldBeEqualTo 1L
    }

    @Test
    fun `indexOfElement returns -1 when sequence falls outside toIndex range`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        // "world" starts at index 6, but toIndex=5 excludes it
        source.indexOfElement("world".encodeUtf8(), 0L, 5L) shouldBeEqualTo -1L
    }

    @Test
    fun `rangeEquals returns true for matching range`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.request(11L)
        source.rangeEquals(6L, "world".encodeUtf8(), 0, 5).shouldBeTrue()
    }

    @Test
    fun `rangeEquals returns false for non-matching range`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello world")
        val source = FakeSuspendedSource(buffer).buffered()
        source.request(11L)
        source.rangeEquals(0L, "world".encodeUtf8(), 0, 5).shouldBeFalse()
    }

    @Test
    fun `peek reads without consuming original source`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello")
        val source = FakeSuspendedSource(buffer).buffered()
        source.request(5L)  // pull data into internal buffer so peek() can see it
        val peeked = source.peek()
        peeked.readUtf8() shouldBeEqualTo "hello"
        source.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `close closes underlying source`() = runSuspendIO {
        val fake = FakeSuspendedSource(Buffer())
        val source = fake.buffered()
        source.close()
        fake.closed.shouldBeTrue()
    }
}

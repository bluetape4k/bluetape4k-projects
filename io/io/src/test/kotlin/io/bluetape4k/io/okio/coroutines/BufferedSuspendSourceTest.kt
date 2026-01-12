package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import okio.EOFException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BufferedSuspendSourceTest: AbstractOkioTest() {

    companion object: KLoggingChannel()

    private class FakeSuspendedSource(private val data: Buffer): SuspendedSource {
        var closed = false
        override suspend fun read(sink: Buffer, byteCount: Long): Long {
            return data.read(sink, byteCount)
        }

        override suspend fun close() {
            closed = true
        }

        override fun timeout() = okio.Timeout.NONE
    }

    @Test
    fun `readByte returns correct byte`() = runSuspendIO {
        val buffer = Buffer().writeByte(0x7F)
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        source.readByte() shouldBeEqualTo 0x7F.toByte()
    }

    @Test
    fun `readShort returns correct short`() = runSuspendIO {
        val buffer = Buffer().writeShort(0x1234)
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        source.readShort() shouldBeEqualTo 0x1234.toShort()
    }

    @Test
    fun `readInt returns correct int`() = runSuspendIO {
        val buffer = Buffer().writeInt(0x12345678)
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        source.readInt() shouldBeEqualTo 0x12345678
    }

    @Test
    fun `readLong returns correct long`() = runSuspendIO {
        val buffer = Buffer().writeLong(0x123456789ABCDEF0)
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        source.readLong() shouldBeEqualTo 0x123456789ABCDEF0L
    }

    @Test
    fun `readByteArray reads all bytes`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = Buffer().write(bytes)
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        source.readByteArray() shouldBeEqualTo bytes
    }

    @Test
    fun `readByteArray with count reads correct bytes`() = runSuspendIO {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = Buffer().write(bytes)
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        source.readByteArray(3) shouldBeEqualTo byteArrayOf(1, 2, 3)
    }

    @Test
    fun `readUtf8 reads string`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello")
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))
        source.readUtf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `readUtf8Line reads line`() = runSuspendIO {
        val buffer = Buffer().writeUtf8("hello\nworld")
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))

        source.readUtf8Line() shouldBeEqualTo "hello"
        source.readUtf8Line() shouldBeEqualTo "world"
        source.readUtf8Line().shouldBeNull() // No more lines
    }

    @Test
    fun `exhausted returns true when source is empty`() = runSuspendIO {
        val buffer = Buffer()
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))

        source.exhausted().shouldBeTrue()
    }

    @Test
    fun `require throws EOFException if not enough bytes`() = runSuspendIO {
        val buffer = Buffer().writeByte(1)
        val source = RealBufferedSuspendedSource(FakeSuspendedSource(buffer))

        assertFailsWith<EOFException> {
            runSuspendIO { source.require(2) }
        }
    }

    @Test
    fun `close closes underlying source`() = runSuspendIO {
        val fake = FakeSuspendedSource(Buffer())
        val source = RealBufferedSuspendedSource(fake)
        source.close()

        fake.closed.shouldBeTrue()
    }
}

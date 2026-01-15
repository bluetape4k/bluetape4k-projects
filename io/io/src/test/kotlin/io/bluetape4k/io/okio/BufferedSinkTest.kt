package io.bluetape4k.io.okio

import io.bluetape4k.io.okio.TestUtil.SEGMENT_SIZE
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import okio.ArrayIndexOutOfBoundsException
import okio.Buffer
import okio.BufferedSink
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.EOFException
import okio.Source
import org.amshove.kluent.fail
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import java.nio.ByteBuffer

class BufferedSinkTest: AbstractOkioTest() {

    companion object: KLogging()

    interface Factory {
        fun create(data: Buffer): BufferedSink

        companion object {
            val BUFFER: Factory = object: Factory {
                override fun create(data: Buffer): BufferedSink = data
                override fun toString() = "Buffer"
            }
            val REAL_BUFFERED_SINK: Factory = object: Factory {
                override fun create(data: Buffer): BufferedSink = data.asBufferedSink()
                override fun toString() = "RealBufferedSink"
            }
        }
    }

    fun factories(): List<Factory> = listOf(Factory.BUFFER, Factory.REAL_BUFFERED_SINK)

    private fun getSink(factory: Factory, data: Buffer): BufferedSink {
        return factory.create(data)
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write nothing`(factory: Factory) {
        val data = Buffer()

        getSink(factory, data).use { sink ->
            sink.writeUtf8("")
            sink.flush()
            data.size shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write bytes`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeByte(0xab)
        sink.writeByte(0xcd)
        sink.flush()

        data.toString() shouldBeEqualTo "[hex=abcd]"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write last byte in segment`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeUtf8("a".repeat(SEGMENT_SIZE - 1))
        sink.writeByte(0x20)  // space
        sink.writeByte(0x21)  // "!"
        sink.flush()

        data.readUtf8(SEGMENT_SIZE - 1L) shouldBeEqualTo "a".repeat(SEGMENT_SIZE - 1)
        data.toString() shouldBeEqualTo "[text= !]"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write Int`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeInt(-0x543210ff)
        sink.writeInt(-0x789abcdf)
        sink.flush()

        data.toString() shouldBeEqualTo "[hex=abcdef0187654321]"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write IntLe`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeIntLe(-0x543210ff)
        sink.writeIntLe(-0x789abcdf)
        sink.flush()

        data.toString() shouldBeEqualTo "[hex=01efcdab21436587]"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write last integer in segment`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)
        sink.writeUtf8("a".repeat(SEGMENT_SIZE - 4))
        sink.writeInt(-0x543210ff)
        sink.writeInt(-0x789abcdf)
        sink.flush()

        data.readUtf8(SEGMENT_SIZE - 4L) shouldBeEqualTo "a".repeat(SEGMENT_SIZE - 4)
        data.toString() shouldBeEqualTo "[hex=abcdef0187654321]"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write Long`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeLong(-0x543210fe789abcdfL)
        sink.writeLong(-0x350145414f4ea400L)
        sink.flush()
        data.toString() shouldBeEqualTo "[hex=abcdef0187654321cafebabeb0b15c00]"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write LongLe`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeLongLe(-0x543210fe789abcdfL)
        sink.writeLongLe(-0x350145414f4ea400L)
        sink.flush()
        data.toString() shouldBeEqualTo "[hex=2143658701efcdab005cb1b0bebafeca]"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write ByteString`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.write(byteStringOf("동해물과 백두산이"))
        sink.flush()

        val readString = data.readByteString()
        log.debug { "Read string=$readString" }
        log.debug { "Read string hex=${readString.hex()}" }

        readString shouldBeEqualTo byteStringOf("동해물과 백두산이")
        readString shouldBeEqualTo "eb8f99ed95b4ebacbceab3bc20ebb0b1eb9190ec82b0ec9db4".decodeHex()
        readString.hex() shouldBeEqualTo "eb8f99ed95b4ebacbceab3bc20ebb0b1eb9190ec82b0ec9db4"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write ByteString Offset`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.write("동해물과 백두산이".encodeUtf8(), 5, 5)
        sink.flush()

        val readString = data.readByteString()
        log.debug { "readString=$readString" }
        readString.hex() shouldBeEqualTo "b4ebacbcea"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write segmented ByteString`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.write(bufferOf("동해물과 백두산이".encodeUtf8()).snapshot())
        sink.flush()

        val readString = data.readByteString()
        log.debug { "readString=$readString, hex=${readString.hex()}" }
        readString shouldBeEqualTo "동해물과 백두산이".encodeUtf8()
        readString.hex() shouldBeEqualTo "eb8f99ed95b4ebacbceab3bc20ebb0b1eb9190ec82b0ec9db4"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write segmented ByteString offset`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.write(bufferOf("동해물과 백두산이".encodeUtf8()).snapshot(), 5, 5)
        sink.flush()

        val readString = data.readByteString()
        log.debug { "readString=$readString, hex=${readString.hex()}" }
        readString.hex() shouldBeEqualTo "b4ebacbcea"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write String UTF8`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeUtf8("동해물과 백두산이")
        sink.flush()

        val readString = data.readByteString()
        log.debug { "Read string=$readString, hex=${readString.hex()}" }

        readString.utf8() shouldBeEqualTo "동해물과 백두산이"
        readString.hex() shouldBeEqualTo "eb8f99ed95b4ebacbceab3bc20ebb0b1eb9190ec82b0ec9db4"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write sub string UTF8`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeUtf8("동해물과 백두산이", 3, 7)
        sink.flush()

        val readString = data.readByteString()
        log.debug { "Read string=$readString, hex=${readString.hex()}" }

        readString.utf8() shouldBeEqualTo "과 백두"
        readString.hex() shouldBeEqualTo "eab3bc20ebb0b1eb9190"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write string with charset`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeString("동해물과 백두산이", Charsets.UTF_32BE)
        sink.flush()

        val readString = data.readByteString()
        log.debug { "Read string=$readString, hex=${readString.hex()}" }

        readString.hex() shouldBeEqualTo "0000b3d90000d5740000bb3c0000acfc000000200000bc310000b4500000c0b00000c774"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write sub string with charset`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeString("동해물과 백두산이", 3, 7, Charsets.UTF_32BE)
        sink.flush()

        val readString = data.readByteString()
        log.debug { "Read string=${readString.utf8()}" }
        log.debug { "Read string hex=${readString.hex()}" }

        readString.hex() shouldBeEqualTo "0000acfc000000200000bc310000b450"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write utf8 string with charset`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeString("동해물과 백두산이", 3, 7, Charsets.UTF_8)
        sink.flush()

        val readString = data.readByteString()
        log.debug { "Read string=${readString.utf8()}" }
        log.debug { "Read string hex=${readString.hex()}" }

        readString.utf8() shouldBeEqualTo "과 백두"
        readString.hex() shouldBeEqualTo "eab3bc20ebb0b1eb9190"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write all`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        val source = bufferOf("동해물과 백두산이")
        sink.writeAll(source) shouldBeEqualTo 25
        source.size shouldBeEqualTo 0
        sink.flush()

        data.readUtf8() shouldBeEqualTo "동해물과 백두산이"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write source`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        val source: Buffer = bufferOf("abcdef")
        sink.write(source, 4)
        sink.flush()

        data.readUtf8() shouldBeEqualTo "abcd"
        source.readUtf8() shouldBeEqualTo "ef"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write source propagates Eof`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        val source: Source = bufferOf("abcd")

        try {
            sink.write(source, 8)  // source 크기보다 커서 EOFException이 발생해야 한다.
            fail("EOFException 이 발생해야 합니다.")
        } catch (expected: EOFException) {
            // Nothing to do
        }

        // 어쨌든 source 의 정보는 쓰여진다.
        sink.flush()
        data.readUtf8() shouldBeEqualTo "abcd"
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `close emits buffered bytes`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        sink.writeByte('a'.code)
        sink.close() // flush() 를 해준다.

        data.readByte().toInt().toChar() shouldBeEqualTo 'a'
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `output stream bounds`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        val out = sink.outputStream()

        // byte array bounds 를 넘어서면 예외가 발생합니다.
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            out.write(ByteArray(100), 50, 51)
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `long decimal string`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        assertLongDecimalString(sink, data, 0)
        assertLongDecimalString(sink, data, Long.MIN_VALUE)
        assertLongDecimalString(sink, data, Long.MAX_VALUE)

        for (i in 1..20) {
            val value = BigInteger.valueOf(10L).pow(i).toLong()
            assertLongDecimalString(sink, data, value - 1)
            assertLongDecimalString(sink, data, value)
        }
    }

    private fun assertLongDecimalString(sink: BufferedSink, data: Buffer, value: Long) {
        sink.writeDecimalLong(value).writeUtf8("zzz").flush()
        val expected = value.toString() + "zzz"
        val actual = data.readUtf8()
        log.debug { "actual=$actual, expected=$expected" }
        actual shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `long hex string`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        assertLongHexString(sink, data, 0)
        assertLongHexString(sink, data, Long.MIN_VALUE)
        assertLongHexString(sink, data, Long.MAX_VALUE)

        for (i in 0..62) {
            assertLongHexString(sink, data, 1L shl i - 1)
            assertLongHexString(sink, data, 1L shl i)
        }
    }

    private fun assertLongHexString(sink: BufferedSink, data: Buffer, value: Long) {
        sink.writeHexadecimalUnsignedLong(value).writeUtf8("zzz").flush()
        val expected = String.format("%x", value) + "zzz"
        val actual = data.readUtf8()
        log.debug { "actual=$actual, expected=$expected" }
        actual shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write nio buffer`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        val expected = Fakers.randomString(512)
        val nioByteBuffer = ByteBuffer.allocate(1024)
        nioByteBuffer.put(expected.toUtf8Bytes())
        nioByteBuffer.flip()

        val byteCount = sink.write(nioByteBuffer)
        byteCount shouldBeEqualTo expected.length
        nioByteBuffer.position() shouldBeEqualTo expected.length
        nioByteBuffer.limit() shouldBeEqualTo expected.length

        sink.flush()
        data.readUtf8() shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `write large nio buffer writes all data`(factory: Factory) {
        val data = Buffer()
        val sink = getSink(factory, data)

        val expected = Fakers.randomString(SEGMENT_SIZE * 3)
        val nioByteBuffer = ByteBuffer.allocateDirect(SEGMENT_SIZE * 4)
        nioByteBuffer.put(expected.toUtf8Bytes())
        nioByteBuffer.flip()

        val byteCount = sink.write(nioByteBuffer)
        byteCount shouldBeEqualTo expected.length
        nioByteBuffer.position() shouldBeEqualTo expected.length
        nioByteBuffer.limit() shouldBeEqualTo expected.length

        sink.flush()
        data.readUtf8() shouldBeEqualTo expected
    }
}

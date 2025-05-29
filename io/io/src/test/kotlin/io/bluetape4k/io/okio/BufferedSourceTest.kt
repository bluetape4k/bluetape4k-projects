package io.bluetape4k.io.okio

import io.bluetape4k.io.okio.TestUtil.SEGMENT_SIZE
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import okio.ArrayIndexOutOfBoundsException
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.EOFException
import okio.ForwardingSink
import okio.ForwardingSource
import okio.Options
import okio.blackholeSink
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer

class BufferedSourceTest {

    companion object: KLogging()

    data class Pipe(
        val name: String,
        val sink: BufferedSink,
        val source: BufferedSource,
    ) {
        override fun toString(): String = name
    }

    interface Factory {
        fun pipe(): Pipe
        val isOneByteAtTime: Boolean

        companion object {
            val BUFFER: Factory = object: Factory {
                override fun pipe(): Pipe {
                    val buffer = Buffer()
                    return Pipe(toString(), buffer, buffer)
                }

                override val isOneByteAtTime: Boolean = false
                override fun toString(): String = "Buffer"
            }

            val REAL_BUFFERED_SOURCE: Factory = object: Factory {
                override fun pipe(): Pipe {
                    val buffer = Buffer()
                    return Pipe(
                        toString(), sink = buffer, source = buffer.asBufferedSource()
                    )
                }

                override val isOneByteAtTime: Boolean = false
                override fun toString(): String = "RealBufferedSource"
            }

            /**
             * A factory deliberately written to create buffers whose internal segments are always 1 byte
             * long. We like testing with these segments because are likely to trigger bugs!
             */
            val ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE: Factory = object: Factory {
                override fun pipe(): Pipe {
                    val buffer = Buffer()
                    val bufferedSource = object: ForwardingSource(buffer) {
                        override fun read(sink: Buffer, byteCount: Long): Long {
                            // Read one byte into a new buffer, then clone it so that the segment is shared.
                            // Shared segments cannot be compacted so we'll get a long chain of short segments.
                            val box = Buffer()
                            val result = super.read(box, minOf(byteCount, 1L))
                            if (result > 0L) sink.write(box.clone(), result)
                            return result
                        }
                    }.buffered()

                    return Pipe(
                        toString(), sink = buffer, source = bufferedSource
                    )
                }

                override val isOneByteAtTime: Boolean = true
                override fun toString(): String = "OneByteAtATimeBufferedSource"
            }

            val ONE_BYTE_AT_A_TIME_BUFFER: Factory = object: Factory {
                override fun pipe(): Pipe {
                    val buffer = Buffer()
                    val sink = object: ForwardingSink(buffer) {
                        override fun write(source: Buffer, byteCount: Long) {
                            // Write each byte into a new buffer, then clone it so that the segments are shared.
                            // Shared segments cannot be compacted so we'll get a long chain of short segments.
                            for (i in 0 until byteCount) {
                                val box = Buffer()
                                box.write(source, 1L)
                                super.write(box.clone(), 1)
                            }
                        }
                    }.buffered()

                    return Pipe(
                        toString(), sink = sink, source = buffer
                    )
                }

                override val isOneByteAtTime: Boolean = true
                override fun toString(): String = "OneByteAtATimeBuffer"
            }

            val PEEK_BUFFER = object: Factory {
                override fun pipe(): Pipe {
                    val buffer = Buffer()
                    return Pipe(
                        toString(), sink = buffer, source = buffer.peek()
                    )
                }

                override val isOneByteAtTime: Boolean = false
                override fun toString(): String = "PeekBuffer"
            }

            val PEEK_BUFFERED_SOURCE: Factory = object: Factory {
                override fun pipe(): Pipe {
                    val buffer = Buffer()
                    return Pipe(
                        toString(), sink = buffer, source = buffer.asBufferedSource().peek()
                    )
                }

                override val isOneByteAtTime: Boolean = false
                override fun toString(): String = "PeekBufferedSource"
            }
        }
    }

    fun factories(): List<Factory> = listOf(
        Factory.BUFFER,
        Factory.REAL_BUFFERED_SOURCE,
        Factory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE,
        Factory.ONE_BYTE_AT_A_TIME_BUFFER,
        Factory.PEEK_BUFFER,
        Factory.PEEK_BUFFERED_SOURCE
    )

    fun pipes(): List<Pipe> = factories().map { it.pipe() }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read bytes`(pipe: Pipe) {
        with(pipe) {
            sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte()))
            sink.emit()

            source.readByte() shouldBeEqualTo 0xab.toByte()
            source.readByte() shouldBeEqualTo 0xcd.toByte()
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read byte too short throws`(pipe: Pipe) {
        assertFailsWith<EOFException> {
            pipe.source.readByte()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read short`(pipe: Pipe) {
        with(pipe) {
            sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte(), 0x01.toByte()))
            sink.emit()

            source.readShort() shouldBeEqualTo 0xabcd.toShort()
            source.readShort() shouldBeEqualTo 0xef01.toShort()
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read shortLe`(pipe: Pipe) {
        with(pipe) {
            sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte(), 0x01.toByte()))
            sink.emit()

            source.readShortLe() shouldBeEqualTo 0xcdab.toShort()
            source.readShortLe() shouldBeEqualTo 0x01ef.toShort()
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read short split across multiple segments`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE - 1))
            sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte()))
            sink.emit()

            // skip the first segment
            source.skip(SEGMENT_SIZE - 1L)
            source.readShort() shouldBeEqualTo 0xabcd.toShort()
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read short too short raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeShort(Short.MAX_VALUE.toInt())
            sink.emit()

            source.readByte()
            assertFailsWith<EOFException> {
                source.readShort()
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read int`(pipe: Pipe) {
        with(pipe) {
            sink.write(
                byteArrayOf(
                    0xab.toByte(),
                    0xcd.toByte(),
                    0xef.toByte(),
                    0x01.toByte(),
                    0x87.toByte(),
                    0x65.toByte(),
                    0x43.toByte(),
                    0x21.toByte()
                )
            )
            sink.emit()

            source.readInt() shouldBeEqualTo -0x543210ff
            source.readInt() shouldBeEqualTo -0x789abcdf
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read intLe`(pipe: Pipe) {
        with(pipe) {
            sink.write(
                byteArrayOf(
                    0xab.toByte(),
                    0xcd.toByte(),
                    0xef.toByte(),
                    0x01.toByte(),
                    0x87.toByte(),
                    0x65.toByte(),
                    0x43.toByte(),
                    0x21.toByte()
                )
            )
            sink.emit()

            source.readIntLe().toLong() shouldBeEqualTo 0x01efcdab
            source.readIntLe().toLong() shouldBeEqualTo 0x21436587
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read int split across multiple segments`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE - 3))
            sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte(), 0x01.toByte()))
            sink.emit()

            // skip the first segment
            source.skip(SEGMENT_SIZE - 3L)
            source.readInt() shouldBeEqualTo -0x543210ff
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read int too short raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeInt(Int.MAX_VALUE)
            sink.emit()

            source.readByte()
            assertFailsWith<EOFException> {
                source.readInt()
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read long`(pipe: Pipe) {
        with(pipe) {
            sink.write(
                byteArrayOf(
                    0xab.toByte(),
                    0xcd.toByte(),
                    0xef.toByte(),
                    0x10.toByte(),
                    0x87.toByte(),
                    0x65.toByte(),
                    0x43.toByte(),
                    0x21.toByte(),
                    0x36.toByte(),
                    0x47.toByte(),
                    0x58.toByte(),
                    0x69.toByte(),
                    0x12.toByte(),
                    0x23.toByte(),
                    0x34.toByte(),
                    0x45.toByte()
                )
            )
            sink.emit()

            source.readLong() shouldBeEqualTo -0x543210ef789abcdfL
            source.readLong() shouldBeEqualTo 0x3647586912233445L
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read longLe`(pipe: Pipe) {
        with(pipe) {
            sink.write(
                byteArrayOf(
                    0xab.toByte(),
                    0xcd.toByte(),
                    0xef.toByte(),
                    0x10.toByte(),
                    0x87.toByte(),
                    0x65.toByte(),
                    0x43.toByte(),
                    0x21.toByte(),
                    0x36.toByte(),
                    0x47.toByte(),
                    0x58.toByte(),
                    0x69.toByte(),
                    0x12.toByte(),
                    0x23.toByte(),
                    0x34.toByte(),
                    0x45.toByte()
                )
            )
            sink.emit()

            source.readLongLe() shouldBeEqualTo 0x2143658710efcdabL
            source.readLongLe() shouldBeEqualTo 0x4534231269584736L
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read long split across multiple segments`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE - 7))
            sink.write(
                byteArrayOf(
                    0xab.toByte(),
                    0xcd.toByte(),
                    0xef.toByte(),
                    0x01.toByte(),
                    0x87.toByte(),
                    0x65.toByte(),
                    0x43.toByte(),
                    0x21.toByte()
                )
            )
            sink.emit()

            // skip the first segment
            source.skip(SEGMENT_SIZE - 7L)
            source.readLong() shouldBeEqualTo -0x543210fe789abcdfL
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read long too short raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeLong(Long.MAX_VALUE)
            sink.emit()

            source.readByte()
            assertFailsWith<EOFException> {
                source.readLong()
            }
        }
    }


    @ParameterizedTest
    @MethodSource("pipes")
    fun `read all`(pipe: Pipe) {
        with(pipe) {
            source.buffer.writeUtf8("abc")

            sink.writeUtf8("def")
            sink.emit()

            val readBuffer = Buffer()
            source.readAll(readBuffer) shouldBeEqualTo 6
            readBuffer.readUtf8() shouldBeEqualTo "abcdef"
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read all exhausted`(pipe: Pipe) {
        val mockSink = MockSink()
        with(pipe) {
            source.readAll(mockSink) shouldBeEqualTo 0L
            source.exhausted().shouldBeTrue()
            mockSink.assertLog()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read exhausted source`(pipe: Pipe) {
        val sink = Buffer()
        sink.writeUtf8("a".repeat(10))

        with(pipe) {
            source.read(sink, 10) shouldBeEqualTo -1L  // source is exhausted
            sink.size shouldBeEqualTo 10L  // 기존 "a".repeat(10) 이 그대로 남아있어야 함
            source.exhausted()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read zero bytes from source`(pipe: Pipe) {
        val sink = Buffer()
        sink.writeUtf8("a".repeat(10))

        // Either 0 or -1 is reasonable here.

        with(pipe) {
            val readCount = source.read(sink, 0)
            readCount shouldBeInRange -1L..0L
            sink.size shouldBeEqualTo 10L
            source.exhausted()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read fully`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(10000))
            sink.emit()

            val readBuffer = Buffer()
            source.readFully(readBuffer, 9999)
            readBuffer.readUtf8() shouldBeEqualTo "a".repeat(9999)
            source.readUtf8() shouldBeEqualTo "a"
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read fully too short throws`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("Hi")
            sink.emit()

            val readBuffer = Buffer()

            assertFailsWith<EOFException> {
                source.readFully(readBuffer, 5)   // 저장된 내용보다 더 많이 읽으려고 하면 EOFException 발생
            }

            // Verify we read all that we could from the source.
            readBuffer.readUtf8() shouldBeEqualTo "Hi"
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read fully byte array`(pipe: Pipe) {
        with(pipe) {
            val data = Buffer()
            data.writeUtf8("Hello").writeUtf8("e".repeat(SEGMENT_SIZE))

            val expected = data.clone().readByteArray()
            sink.write(data, data.size)
            sink.emit()

            val bytes = ByteArray(SEGMENT_SIZE + 5)
            source.readFully(bytes)
            bytes shouldBeEqualTo expected
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read fully byte array when too short then throws`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("Hello")
            sink.emit()

            val bytes = ByteArray(6)
            assertFailsWith<EOFException> {
                // bytes 크기만큼 읽으려고 하지만, source의 크기가 그에 미치지 못하면 EOFException 발생
                source.readFully(bytes)
            }

            // Verify we read all that we could from the source.
            bytes shouldBeEqualTo "Hello".toByteArray() + byteArrayOf(0)
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `read into byte array`(factory: Factory) {
        val pipe = factory.pipe()
        with(pipe) {
            sink.writeUtf8("abcd")
            sink.emit()

            val bytes = ByteArray(3)
            val read = source.read(bytes)
            if (factory.isOneByteAtTime) {
                read shouldBeEqualTo 1
                bytes shouldBeEqualTo byteArrayOf('a'.code.toByte(), 0, 0)
            } else {
                read shouldBeEqualTo 3
                bytes shouldBeEqualTo "abc".toUtf8Bytes()
            }
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `read into byte array not enough`(factory: Factory) {
        val pipe = factory.pipe()
        with(pipe) {
            sink.writeUtf8("abcd")   // size = 4
            sink.emit()

            val bytes = ByteArray(5)    // size = 5
            val read = source.read(bytes)
            if (factory.isOneByteAtTime) {
                read shouldBeEqualTo 1
                bytes shouldBeEqualTo byteArrayOf('a'.code.toByte(), 0, 0, 0, 0)
            } else {
                read shouldBeEqualTo 4
                bytes shouldBeEqualTo "abcd".toUtf8Bytes() + byteArrayOf(0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `read into byte array with offset and count`(factory: Factory) {
        val pipe = factory.pipe()
        with(pipe) {
            sink.writeUtf8("abcd")   // size = 4
            sink.emit()

            val bytes = ByteArray(7)
            val read = source.read(bytes, 2, 3)
            if (factory.isOneByteAtTime) {
                read shouldBeEqualTo 1
                bytes shouldBeEqualTo byteArrayOf(0, 0, 'a'.code.toByte(), 0, 0, 0, 0)
            } else {
                read shouldBeEqualTo 3
                bytes shouldBeEqualTo byteArrayOf(0, 0) + "abc".toUtf8Bytes() + byteArrayOf(0, 0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read byte array`(pipe: Pipe) {
        with(pipe) {
            val string = "abcd" + "e".repeat(SEGMENT_SIZE)
            sink.writeUtf8(string)
            sink.emit()

            source.readByteArray() shouldBeEqualTo string.toUtf8Bytes()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read byte array partial`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abcd")
            sink.emit()

            source.readByteArray(3) shouldBeEqualTo byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte())
            source.readUtf8(1) shouldBeEqualTo "d"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read byte array two short throws`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abc")
            sink.emit()

            assertFailsWith<EOFException> {
                source.readByteArray(4)
            }
            source.readUtf8() shouldBeEqualTo "abc"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read byteString`(pipe: Pipe) {
        with(pipe) {
            val string = "abcd" + "e".repeat(SEGMENT_SIZE)
            sink.writeUtf8(string)
            sink.emit()

            source.readByteString().utf8() shouldBeEqualTo string
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read byteString partial`(pipe: Pipe) {
        with(pipe) {
            val string = "abcd" + "e".repeat(SEGMENT_SIZE)
            sink.writeUtf8(string)
            sink.emit()

            source.readByteString(3).utf8() shouldBeEqualTo "abc"
            source.readUtf8(1) shouldBeEqualTo "d"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read byteString too short - raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abc")
            sink.emit()

            assertFailsWith<EOFException> {
                source.readByteString(4)
            }
            source.readUtf8() shouldBeEqualTo "abc"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read specific charset partial`(pipe: Pipe) {
        with(pipe) {
            val str =
                "0000007600000259000002c80000006c000000e40000007300000259000002cc000000720000006100000070000000740000025900000072".decodeHex()
            log.debug { "decode hex to utf32=${str.string(Charsets.UTF_32)}" }
            sink.write(str)
            sink.emit()

            source.readString(7 * 4L, Charsets.UTF_32) shouldBeEqualTo "vəˈläsə"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read specific charset`(pipe: Pipe) {
        with(pipe) {
            val str =
                "0000007600000259000002c80000006c000000e40000007300000259000002cc000000720000006100000070000000740000025900000072".decodeHex()
            log.debug { "decode hex to utf32=${str.string(Charsets.UTF_32)}" }
            sink.write(str)
            sink.emit()

            source.readString(Charsets.UTF_32) shouldBeEqualTo "vəˈläsəˌraptər"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read string too short then throw exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeString("abc", Charsets.US_ASCII)
            sink.emit()

            assertFailsWith<EOFException> {
                source.readString(4, Charsets.US_ASCII)
            }

            source.readUtf8() shouldBeEqualTo "abc"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read utf8 spans segments`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE * 2))
            sink.emit()

            source.skip(SEGMENT_SIZE - 1L)
            source.readUtf8(2) shouldBeEqualTo "aa"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read utf8 segment`(pipe: Pipe) {
        with(pipe) {
            val expected = "a".repeat(SEGMENT_SIZE)
            sink.writeUtf8(expected)
            sink.emit()

            source.readUtf8(SEGMENT_SIZE.toLong()) shouldBeEqualTo expected
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read utf8 partial buffer`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE + 20))
            sink.emit()

            source.readUtf8(SEGMENT_SIZE + 10L) shouldBeEqualTo "a".repeat(SEGMENT_SIZE + 10)
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read utf8 entire buffer`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE * 2))
            sink.emit()

            source.readUtf8() shouldBeEqualTo "a".repeat(SEGMENT_SIZE * 2)
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read utf8 too short then throw exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abc")
            sink.emit()

            assertFailsWith<EOFException> {
                source.readUtf8(4L)
            }

            source.readUtf8() shouldBeEqualTo "abc"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `read with skip`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a")
            sink.writeUtf8("b".repeat(SEGMENT_SIZE))
            sink.writeUtf8("c")
            sink.emit()

            source.skip(1)
            source.readByte() shouldBeEqualTo 'b'.code.toByte()
            source.skip(SEGMENT_SIZE - 2L)
            source.readByte() shouldBeEqualTo 'b'.code.toByte()
            source.skip(1)
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `skip insufficient data`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a")
            sink.emit()

            assertFailsWith<EOFException> {
                source.skip(2)
            }

            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `source indexOf`(pipe: Pipe) {
        with(pipe) {
            // The segment is empty.
            source.indexOf('a'.code.toByte()) shouldBeEqualTo -1L

            // The segment has one value.
            sink.writeUtf8("a")
            sink.emit()
            source.indexOf('a'.code.toByte()) shouldBeEqualTo 0L
            source.indexOf('b'.code.toByte()) shouldBeEqualTo -1L

            // The segment has lots of data.
            sink.writeUtf8("b".repeat(SEGMENT_SIZE - 2))  // ab...b
            sink.emit()
            source.indexOf('a'.code.toByte()) shouldBeEqualTo 0L
            source.indexOf('b'.code.toByte()) shouldBeEqualTo 1L
            source.indexOf('c'.code.toByte()) shouldBeEqualTo -1L

            // The segment doesn't start at 0, it starts at 2.
            source.skip(2)  // b...b
            source.indexOf('a'.code.toByte()) shouldBeEqualTo -1L
            source.indexOf('b'.code.toByte()) shouldBeEqualTo 0L
            source.indexOf('c'.code.toByte()) shouldBeEqualTo -1L

            // The segment is full.
            sink.writeUtf8("c")  // b...bc
            sink.emit()
            source.indexOf('a'.code.toByte()) shouldBeEqualTo -1L
            source.indexOf('b'.code.toByte()) shouldBeEqualTo 0L
            source.indexOf('c'.code.toByte()) shouldBeEqualTo SEGMENT_SIZE - 3L

            // The segment doesn't start at 2, it starts at 4.
            source.skip(2)  // b...bc
            source.indexOf('a'.code.toByte()) shouldBeEqualTo -1L
            source.indexOf('b'.code.toByte()) shouldBeEqualTo 0L
            source.indexOf('c'.code.toByte()) shouldBeEqualTo SEGMENT_SIZE - 5L

            // Tow segments
            sink.writeUtf8("d")  // b...bcd, d is in the 2nd segment
            sink.emit()

            source.indexOf('d'.code.toByte()) shouldBeEqualTo SEGMENT_SIZE - 4L
            source.indexOf('e'.code.toByte()) shouldBeEqualTo -1L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of byte with start offset`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a").writeUtf8("b".repeat(SEGMENT_SIZE)).writeUtf8("c")
            sink.emit()

            source.indexOf('a'.code.toByte(), 0L) shouldBeEqualTo 0
            source.indexOf('b'.code.toByte(), 15L) shouldBeEqualTo 15
            source.indexOf('c'.code.toByte(), SEGMENT_SIZE.toLong()) shouldBeEqualTo SEGMENT_SIZE + 1L
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `index of byte with both offsets`(factory: Factory) {
        Assumptions.assumeFalse { factory.isOneByteAtTime }

        val pipe = factory.pipe()
        with(pipe) {
            val a = 'a'.code.toByte()
            val c = 'c'.code.toByte()
            val size = SEGMENT_SIZE * 5
            val bytes = ByteArray(size) { a }

            // Thease are tricky place where the buffer start, ends, or segments come together.
            val points = intArrayOf(
                0, 1, 2,
                SEGMENT_SIZE - 1, SEGMENT_SIZE, SEGMENT_SIZE + 1,
                size / 2 - 1, size / 2, size / 2 + 1,
                size - SEGMENT_SIZE - 1, size - SEGMENT_SIZE, size - SEGMENT_SIZE + 1,
                size - 3, size - 2, size - 1,
            )

            // In each iteration, we write c to the known point and then search for it using different
            // windows. Some of the windows don't overlap with c's position, and therefore a match shouldn't
            // be found.
            points.forEach { p ->
                bytes[p] = c
                sink.write(bytes)
                sink.emit()

                source.indexOf(c, 0, size.toLong()) shouldBeEqualTo p.toLong()
                source.indexOf(c, 0, p + 1L) shouldBeEqualTo p.toLong()
                source.indexOf(c, p + 0L, size.toLong()) shouldBeEqualTo p.toLong()
                source.indexOf(c, p + 0L, p + 1L) shouldBeEqualTo p.toLong()
                source.indexOf(c, (p / 2).toLong(), (p * 2 + 1).toLong()) shouldBeEqualTo p.toLong()

                source.indexOf(c, 0, (p / 2).toLong()) shouldBeEqualTo -1L
                source.indexOf(c, 0, p.toLong()) shouldBeEqualTo -1L
                source.indexOf(c, 0, 0) shouldBeEqualTo -1L
                source.indexOf(c, p.toLong(), p.toLong()) shouldBeEqualTo -1L

                // Reset.
                source.readUtf8()
                bytes[p] = a
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of byte invailid bounds must throw exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abc")
            sink.emit()

            assertFailsWith<IllegalArgumentException> {
                source.indexOf('a'.code.toByte(), -1)
            }

            assertFailsWith<IllegalArgumentException> {
                source.indexOf('a'.code.toByte(), 10, 0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of byte string`(pipe: Pipe) {
        with(pipe) {
            source.indexOf("flop".encodeUtf8()) shouldBeEqualTo -1L

            sink.writeUtf8("flip flop")
            sink.emit()
            source.indexOf("flop".encodeUtf8()) shouldBeEqualTo 5L
            source.readUtf8() // Clear stream

            // Make sure we backtrack and resume searching after partial match.
            sink.writeUtf8("hi hi hi hey")
            sink.emit()
            source.indexOf("hi hi hey".encodeUtf8()) shouldBeEqualTo 3L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of ByteString at segment boundary`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE - 1))
            sink.writeUtf8("bcd")
            sink.emit()

            source.indexOf("aabc".encodeUtf8(), SEGMENT_SIZE - 4L) shouldBeEqualTo SEGMENT_SIZE - 3L
            source.indexOf("aabc".encodeUtf8(), SEGMENT_SIZE - 3L) shouldBeEqualTo SEGMENT_SIZE - 3L
            source.indexOf("abcd".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE - 2L
            source.indexOf("abc".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE - 2L
            source.indexOf("ab".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE - 2L

            source.indexOf("a".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE - 2L
            source.indexOf("bc".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE - 1L
            source.indexOf("b".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE - 1L

            source.indexOf("c".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE.toLong()
            source.indexOf("c".encodeUtf8(), SEGMENT_SIZE.toLong()) shouldBeEqualTo SEGMENT_SIZE.toLong()

            source.indexOf("d".encodeUtf8(), SEGMENT_SIZE - 2L) shouldBeEqualTo SEGMENT_SIZE + 1L
            source.indexOf("d".encodeUtf8(), SEGMENT_SIZE + 1L) shouldBeEqualTo SEGMENT_SIZE + 1L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of does not wrap around`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE - 1))
            sink.writeUtf8("bcd")
            sink.emit()

            source.indexOf("abcda".encodeUtf8(), SEGMENT_SIZE - 3L) shouldBeEqualTo -1L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of ByteString with offset`(pipe: Pipe) {
        with(pipe) {
            source.indexOf("flop".encodeUtf8(), 1L) shouldBeEqualTo -1L

            sink.writeUtf8("flop flip flop")
            sink.emit()
            source.indexOf("flop".encodeUtf8(), 1L) shouldBeEqualTo 10L
            source.readUtf8()// Clear stream

            // Make sure we backtrack and resume searching after partial match.
            sink.writeUtf8("hi hi hi hi key")
            sink.emit()
            source.indexOf("hi hi key".encodeUtf8(), 1L) shouldBeEqualTo 6L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of ByteString throws invalid arguments`(pipe: Pipe) {
        with(pipe) {
            assertFailsWith<IllegalArgumentException> {
                source.indexOf(ByteString.of())
            }.message!!.shouldNotBeEmpty()  // shouldBeEqualTo "byteCount == 0"

            assertFailsWith<IllegalArgumentException> {
                source.indexOf("hi".encodeUtf8(), -1L)
            }.message shouldBeEqualTo "fromIndex < 0: -1"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of ByteString across segment boundaries`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE * 2 - 3))
            sink.writeUtf8("bcdefg")
            sink.emit()

            source.indexOf("ab".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 4L
            source.indexOf("abc".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 4L
            source.indexOf("abcd".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 4L
            source.indexOf("abcde".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 4L
            source.indexOf("abcdef".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 4L
            source.indexOf("abcdefg".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 4L
            source.indexOf("bcdefg".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 3L
            source.indexOf("cdefg".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 2L
            source.indexOf("defg".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 - 1L
            source.indexOf("efg".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2L
            source.indexOf("fg".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 + 1L
            source.indexOf("g".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE * 2 + 2L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of element`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a").writeUtf8("b".repeat(SEGMENT_SIZE)).writeUtf8("c")
            sink.emit()

            // 하나의 요소라도 있다면 그 위치를 반환한다 
            source.indexOfElement("DEFGaHIJK".encodeUtf8()) shouldBeEqualTo 0L
            source.indexOfElement("DEFGHIJKb".encodeUtf8()) shouldBeEqualTo 1L
            source.indexOfElement("cDEFGHIJK".encodeUtf8()) shouldBeEqualTo SEGMENT_SIZE + 1L
            source.indexOfElement("DEFbGHIc".encodeUtf8()) shouldBeEqualTo 1L
            source.indexOfElement("DEFGHIJK".encodeUtf8()) shouldBeEqualTo -1L
            source.indexOfElement("".encodeUtf8()) shouldBeEqualTo -1L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of element with offset`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a").writeUtf8("b".repeat(SEGMENT_SIZE)).writeUtf8("c")
            sink.emit()

            source.indexOfElement("DEFGaHIJK".encodeUtf8(), 1L) shouldBeEqualTo -1L
            source.indexOfElement("DEFGHIJKb".encodeUtf8(), 15L) shouldBeEqualTo 15L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of byte with from index`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("aaa")
            sink.emit()

            source.indexOf('a'.code.toByte()) shouldBeEqualTo 0L
            source.indexOf('a'.code.toByte(), 0L) shouldBeEqualTo 0L
            source.indexOf('a'.code.toByte(), 1L) shouldBeEqualTo 1L
            source.indexOf('a'.code.toByte(), 2L) shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `index of ByteString with from index`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("aaa")
            sink.emit()

            source.indexOf("a".encodeUtf8()) shouldBeEqualTo 0L
            source.indexOf("a".encodeUtf8(), 0L) shouldBeEqualTo 0L
            source.indexOf("a".encodeUtf8(), 1L) shouldBeEqualTo 1L
            source.indexOf("a".encodeUtf8(), 2L) shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `indexOfElement with from index`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("aaa")
            sink.emit()

            source.indexOfElement("a".encodeUtf8()) shouldBeEqualTo 0L
            source.indexOfElement("a".encodeUtf8(), 0L) shouldBeEqualTo 0L
            source.indexOfElement("a".encodeUtf8(), 1L) shouldBeEqualTo 1L
            source.indexOfElement("a".encodeUtf8(), 2L) shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `source request`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a").writeUtf8("b".repeat(SEGMENT_SIZE)).writeUtf8("c")
            sink.emit()

            source.request(SEGMENT_SIZE + 2L).shouldBeTrue()
            source.request(SEGMENT_SIZE + 3L).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `source require`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a").writeUtf8("b".repeat(SEGMENT_SIZE)).writeUtf8("c")
            sink.emit()

            source.require(SEGMENT_SIZE + 2L)

            assertFailsWith<EOFException> {
                source.require(SEGMENT_SIZE + 3L)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `use input stream`(factory: Factory) {
        val pipe = factory.pipe()
        with(pipe) {
            sink.writeUtf8("abc")
            sink.emit()

            val input = source.inputStream()
            val bytes = "zzz".toUtf8Bytes()
            var read = input.read(bytes)

            if (factory.isOneByteAtTime) {
                read shouldBeEqualTo 1
                bytes shouldBeEqualTo "azz".toUtf8Bytes()

                read = input.read(bytes)
                read shouldBeEqualTo 1
                bytes shouldBeEqualTo "bzz".toUtf8Bytes()

                read = input.read(bytes)
                read shouldBeEqualTo 1
                bytes shouldBeEqualTo "czz".toUtf8Bytes()
            } else {
                read shouldBeEqualTo 3
                bytes shouldBeEqualTo "abc".toUtf8Bytes()
            }

            input.read() shouldBeEqualTo -1
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `inputStream offset count`(factory: Factory) {
        val pipe = factory.pipe()
        with(pipe) {
            sink.writeUtf8("abcde")
            sink.emit()

            val input = source.inputStream()
            val bytes = "zzzzz".toUtf8Bytes()
            val read = input.read(bytes, 1, 3)
            if (factory.isOneByteAtTime) {
                read shouldBeEqualTo 1
                bytes shouldBeEqualTo "zazzz".toUtf8Bytes()
            } else {
                read shouldBeEqualTo 3
                bytes shouldBeEqualTo "zabcz".toUtf8Bytes()
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `inputStream with skip`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abcde")
            sink.emit()

            val input = source.inputStream()
            input.skip(4) shouldBeEqualTo 4L
            input.read().toChar() shouldBeEqualTo 'e'

            sink.writeUtf8("abcde")
            sink.emit()

            input.skip(10) shouldBeEqualTo 5L  // input 크기보다 크게 skip 하더라도 input 크기만큼만 skip 된다
            input.skip(1) shouldBeEqualTo 0L   // 이미 EOF 이므로 skip 불가능
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `inputStream char by char`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abc")
            sink.emit()

            val input = source.inputStream()
            input.read() shouldBeEqualTo 'a'.code
            input.read() shouldBeEqualTo 'b'.code
            input.read() shouldBeEqualTo 'c'.code
            input.read() shouldBeEqualTo -1
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `inputStream bounds`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(100))
            sink.emit()

            val input = source.inputStream()
            assertFailsWith<ArrayIndexOutOfBoundsException> {
                input.read(ByteArray(100), 50, 51)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long hex string`(pipe: Pipe) {
        pipe.assertLongHexString("8000000000000000", -0x7fffffffffffffffL - 1L)
        pipe.assertLongHexString("fffffffffffffffe", -0x2L)
        pipe.assertLongHexString("FFFFFFFFFFFFFFFe", -0x2L)
        pipe.assertLongHexString("ffffffffffffffff", -0x1L)
        pipe.assertLongHexString("FFFFFFFFFFFFFFFF", -0x1L)
        pipe.assertLongHexString("0000000000000000", 0x0)
        pipe.assertLongHexString("0000000000000001", 0x1)
        pipe.assertLongHexString("7999999999999999", 0x7999999999999999L)
        pipe.assertLongHexString("FF", 0xFF)
        pipe.assertLongHexString("0000000000000001", 0x1)
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `hex string with many leading zeros`(pipe: Pipe) {
        pipe.assertLongHexString("00000000000000001", 0x1)
        pipe.assertLongHexString("0000000000000000ffffffffffffffff", -0x1L)
        pipe.assertLongHexString("00000000000000007fffffffffffffff", 0x7fffffffffffffffL)
        pipe.assertLongHexString("0".repeat(SEGMENT_SIZE + 1) + "1", 0x1)
    }

    private fun Pipe.assertLongHexString(str: String, expected: Long) {
        sink.writeUtf8(str)
        sink.emit()

        val actual = source.readHexadecimalUnsignedLong()
        actual shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long hex string across segment`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("a".repeat(SEGMENT_SIZE - 8)).writeUtf8("FFFFFFFFFFFFFFFF")
            sink.emit()

            source.skip(SEGMENT_SIZE - 8L)
            source.readHexadecimalUnsignedLong() shouldBeEqualTo -1L
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long hex string too long raise exception`(pipe: Pipe) {
        with(pipe) {
            val content = "fffffffffffffffff"
            sink.writeUtf8(content)
            sink.emit()

            assertFailsWith<NumberFormatException> {
                source.readHexadecimalUnsignedLong()
            }.message shouldBeEqualTo "Number too large: $content"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long hex string too short raise exception`(pipe: Pipe) {
        with(pipe) {
            val content = " "
            sink.writeUtf8(content)
            sink.emit()

            assertFailsWith<NumberFormatException> {
                source.readHexadecimalUnsignedLong()
            }.message shouldBeEqualTo "Expected leading [0-9a-fA-F] character but was 0x20"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long hex empty source raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("")
            sink.emit()

            assertFailsWith<EOFException> {
                source.readHexadecimalUnsignedLong()
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long decimal string`(pipe: Pipe) {
        pipe.assertLongDecimalString("-9223372036854775808", -9223372036854775807L - 1L)
        pipe.assertLongDecimalString("-1", -1L)
        pipe.assertLongDecimalString("0", 0L)
        pipe.assertLongDecimalString("1", 1L)
        pipe.assertLongDecimalString("9223372036854775807", 9223372036854775807L)
        pipe.assertLongDecimalString("00000001", 1L)
        pipe.assertLongDecimalString("-000001", -1L)
    }

    private fun Pipe.assertLongDecimalString(s: String, expected: Long) {
        sink.writeUtf8(s)
        sink.writeUtf8("zzz")
        sink.emit()

        val actual = source.readDecimalLong()
        actual shouldBeEqualTo expected
        source.readUtf8() shouldBeEqualTo "zzz"
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long decimal string across segment`(pipe: Pipe) {
        with(pipe) {
            val decimal = "1234567890123456"
            sink.writeUtf8("a".repeat(SEGMENT_SIZE - 8)).writeUtf8(decimal).writeUtf8("zzz")
            sink.emit()

            source.skip(SEGMENT_SIZE - 8L)
            source.readDecimalLong() shouldBeEqualTo decimal.toLong()
            source.readUtf8() shouldBeEqualTo "zzz"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long decimal string too long raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("12345678901234567890")
            sink.emit()

            assertFailsWith<NumberFormatException> {
                source.readDecimalLong()
            }.message shouldBeEqualTo "Number too large: 12345678901234567890"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long decimal string is too high raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("9223372036854775808")  // Right size but cannot fit.
            sink.emit()

            assertFailsWith<NumberFormatException> {
                source.readDecimalLong()
            }.message shouldBeEqualTo "Number too large: 9223372036854775808"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long decimal string is too low raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("-9223372036854775809")  // Right size but cannot fit.
            sink.emit()

            assertFailsWith<NumberFormatException> {
                source.readDecimalLong()
            }.message shouldBeEqualTo "Number too large: -9223372036854775809"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long decimal string is too short raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8(" ")
            sink.emit()

            assertFailsWith<NumberFormatException> {
                source.readDecimalLong()
            }.message shouldBeEqualTo "Expected a digit or '-' but was 0x20"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `long decimal string is emtpy raise exception`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("")
            sink.emit()

            assertFailsWith<EOFException> {
                source.readDecimalLong()
            }
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `code points`(pipe: Pipe) {
        with(pipe) {
            sink.write("7f".decodeHex())
            sink.emit()
            source.readUtf8CodePoint() shouldBeEqualTo 0x7f

            sink.write("dfbf".decodeHex())
            sink.emit()
            source.readUtf8CodePoint().toLong() shouldBeEqualTo 0x07ffL

            sink.write("efbfbf".decodeHex())
            sink.emit()
            source.readUtf8CodePoint().toLong() shouldBeEqualTo 0xffffL

            sink.write("f48fbfbf".decodeHex())
            sink.emit()
            source.readUtf8CodePoint().toLong() shouldBeEqualTo 0x10ffffL

            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `decimal string with many leading zeros`(pipe: Pipe) {
        with(pipe) {
            assertLongDecimalString("00000000000000001", 1)
            assertLongDecimalString("00000000000000009223372036854775807", 9223372036854775807L)
            assertLongDecimalString("-00000000000000009223372036854775808", -9223372036854775807L - 1L)
            assertLongDecimalString("0".repeat(SEGMENT_SIZE + 1) + "1", 1)
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `select by options`(pipe: Pipe) {
        val options = Options.of(
            "ROCK".encodeUtf8(), "SCISSORS".encodeUtf8(), "PAPER".encodeUtf8()
        )
        with(pipe) {
            sink.writeUtf8("PAPER,SCISSORS,ROCK")
            sink.emit()

            source.select(options) shouldBeEqualTo 2
            source.readByte() shouldBeEqualTo ','.code.toByte()
            source.select(options) shouldBeEqualTo 1
            source.readByte() shouldBeEqualTo ','.code.toByte()
            source.select(options) shouldBeEqualTo 0
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `select spanning multiple segments`(pipe: Pipe) {
        with(pipe) {
            val commonPrefix = TestUtil.randomBytes(SEGMENT_SIZE + 10)
            val a = bufferOf(commonPrefix).writeUtf8("a").readByteString()
            val bc = bufferOf(commonPrefix).writeUtf8("bc").readByteString()
            val bd = bufferOf(commonPrefix).writeUtf8("bd").readByteString()
            val options = Options.of(a, bc, bd)

            sink.write(bd)
            sink.write(a)
            sink.write(bc)
            sink.emit()

            source.select(options) shouldBeEqualTo 2
            source.select(options) shouldBeEqualTo 0
            source.select(options) shouldBeEqualTo 1
            source.exhausted()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `select not found`(pipe: Pipe) {
        val options = Options.of(
            "ROCK".encodeUtf8(), "SCISSORS".encodeUtf8(), "PAPER".encodeUtf8()
        )
        with(pipe) {
            sink.writeUtf8("SPOCK")
            sink.emit()

            source.select(options) shouldBeEqualTo -1
            source.readUtf8() shouldBeEqualTo "SPOCK"
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `select values have common prefix`(pipe: Pipe) {
        val options = Options.of(
            "abcd".encodeUtf8(), "abce".encodeUtf8(), "abcc".encodeUtf8()
        )
        with(pipe) {
            sink.writeUtf8("abcc").writeUtf8("abcd").writeUtf8("abce")
            sink.emit()

            source.select(options) shouldBeEqualTo 2
            source.select(options) shouldBeEqualTo 0
            source.select(options) shouldBeEqualTo 1
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `select longer than source`(pipe: Pipe) {
        val options = Options.of(
            "abcd".encodeUtf8(), "abce".encodeUtf8(), "abcc".encodeUtf8()
        )
        with(pipe) {
            sink.writeUtf8("abc")
            sink.emit()

            source.select(options) shouldBeEqualTo -1
            source.readUtf8() shouldBeEqualTo "abc"
            source.exhausted().shouldBeTrue()
        }
    }


    @ParameterizedTest
    @MethodSource("pipes")
    fun `select returns first ByteString that matches`(pipe: Pipe) {
        val options = Options.of(
            "abcd".encodeUtf8(), "abc".encodeUtf8(), "abcde".encodeUtf8()
        )
        with(pipe) {
            sink.writeUtf8("abcdef")
            sink.emit()

            source.select(options) shouldBeEqualTo 0
            source.readUtf8() shouldBeEqualTo "ef"
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `select from empty source`(pipe: Pipe) {
        val options = Options.of(
            "abc".encodeUtf8(),
            "def".encodeUtf8(),
        )
        with(pipe) {
            source.select(options) shouldBeEqualTo -1
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `peek usage`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abcdefghi")
            sink.emit()

            source.readUtf8(3) shouldBeEqualTo "abc"

            val peek = source.peek()
            peek.readUtf8(3) shouldBeEqualTo "def"
            peek.readUtf8(3) shouldBeEqualTo "ghi"
            peek.request(1).shouldBeFalse()

            source.readUtf8(3) shouldBeEqualTo "def"
            source.readUtf8(3) shouldBeEqualTo "ghi"
            source.request(1).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `peek multiple`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abcdefghi")
            sink.emit()

            source.readUtf8(3L) shouldBeEqualTo "abc"
            val peek1 = source.peek()
            val peek2 = source.peek()

            peek1.readUtf8(3L) shouldBeEqualTo "def"

            peek2.readUtf8(3L) shouldBeEqualTo "def"
            peek2.readUtf8(3L) shouldBeEqualTo "ghi"
            peek2.request(1).shouldBeFalse()

            peek1.readUtf8(3) shouldBeEqualTo "ghi"
            peek1.request(1).shouldBeFalse()

            source.readUtf8(3) shouldBeEqualTo "def"
            source.readUtf8(3) shouldBeEqualTo "ghi"
            source.request(1).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `peek large`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abcdef")
            sink.writeUtf8("g".repeat(2 * SEGMENT_SIZE))
            sink.writeUtf8("hij")
            sink.emit()

            source.readUtf8(3) shouldBeEqualTo "abc"
            val peek = source.peek()
            peek.readUtf8(3) shouldBeEqualTo "def"

            peek.skip(2L * SEGMENT_SIZE)
            peek.readUtf8(3) shouldBeEqualTo "hij"
            peek.request(1).shouldBeFalse()

            source.readUtf8(3) shouldBeEqualTo "def"
            source.skip(2L * SEGMENT_SIZE)
            source.readUtf8(3) shouldBeEqualTo "hij"
            source.exhausted().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `peek invalid`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abcdefghi")
            sink.emit()

            source.readUtf8(3) shouldBeEqualTo "abc"

            val peek = source.peek()
            peek.readUtf8(3) shouldBeEqualTo "def"
            peek.readUtf8(3) shouldBeEqualTo "ghi"
            peek.request(1).shouldBeFalse()

            source.readUtf8(3) shouldBeEqualTo "def"

            assertFailsWith<IllegalStateException> {
                peek.readUtf8()
            }.message shouldBeEqualTo "Peek source is invalid because upstream source was used"
        }
    }


    @ParameterizedTest
    @MethodSource("pipes")
    fun `peek segment then invalid`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("abc")
            sink.writeUtf8("d".repeat(2 * SEGMENT_SIZE))
            sink.emit()

            source.readUtf8(3) shouldBeEqualTo "abc"

            // peek a little data and skip the rest of the upstream source
            val peek = source.peek()
            peek.readUtf8(3) shouldBeEqualTo "ddd"
            source.readAll(blackholeSink())

            // skip the rest of the buffered data
            peek.skip(peek.buffer.size)

            assertFailsWith<IllegalStateException> {
                peek.readByte()
            }.message shouldBeEqualTo "Peek source is invalid because upstream source was used"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `peek doesnt read too much`(pipe: Pipe) {
        with(pipe) {
            // 6 bytes in source's buffer plus 3 bytes upstream.
            sink.writeUtf8("abcdef")
            sink.emit()

            source.require(6L)
            sink.writeUtf8("ghi")
            sink.emit()

            val peek = source.peek()

            // Read 3 bytes. This reads some of the buffered data.
            peek.request(3).shouldBeTrue()
            if (source !is Buffer) {
                source.buffer.size shouldBeEqualTo 6L
                peek.buffer.size shouldBeEqualTo 6L
            }
            peek.readUtf8(3) shouldBeEqualTo "abc"

            // Read 3 more bytes. This exhausts the buffered data.
            peek.request(3).shouldBeTrue()
            if (source !is Buffer) {
                source.buffer.size shouldBeEqualTo 6L
                peek.buffer.size shouldBeEqualTo 3L
            }
            peek.readUtf8(3) shouldBeEqualTo "def"

            // Read 3 more bytes. This draws new bytes.
            peek.request(3).shouldBeTrue()
            source.buffer.size shouldBeEqualTo 9L
            peek.buffer.size shouldBeEqualTo 3L
            peek.readUtf8(3) shouldBeEqualTo "ghi"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `range equals`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("A man, a plan, a canal. Panama.")
            sink.emit()

            source.rangeEquals(7, "a plan".encodeUtf8()).shouldBeTrue()
            source.rangeEquals(0, "A man".encodeUtf8()).shouldBeTrue()
            source.rangeEquals(24, "Panama".encodeUtf8()).shouldBeTrue()
            source.rangeEquals(24, "Panama. Panama. Panama.".encodeUtf8()).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `range equals with offset and count`(pipe: Pipe) {
        with(pipe) {
            sink.writeUtf8("A man, a plan, a canal. Panama.")
            sink.emit()

            source.rangeEquals(7, "aaa plannn".encodeUtf8(), 2, 6).shouldBeTrue()
            source.rangeEquals(0, "AAA mannn".encodeUtf8(), 2, 5).shouldBeTrue()
            source.rangeEquals(24, "PPPanamaaa".encodeUtf8(), 2, 6).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `range equals only reads until mismatch`(factory: Factory) {
        Assumptions.assumeTrue(factory === Factory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)   // Other sources read in chunks anyway.

        val pipe = factory.pipe()
        with(pipe) {
            sink.writeUtf8("A man, a plan, a canal. Panama.")
            sink.emit()

            source.rangeEquals(0, "A man.".encodeUtf8()).shouldBeFalse()
            source.buffer.readUtf8() shouldBeEqualTo "A man,"
        }
    }

    @ParameterizedTest
    @MethodSource("pipes")
    fun `range equals argument validation`(pipe: Pipe) {
        with(pipe) {
            // Negative source offset.
            source.rangeEquals(-1, "A".encodeUtf8()).shouldBeFalse()
            // Negative bytes offset.
            source.rangeEquals(0, "A".encodeUtf8(), -1, 1).shouldBeFalse()
            // Bytes offset longer than bytes length.
            source.rangeEquals(0, "A".encodeUtf8(), 2, 1).shouldBeFalse()
            // Negative byte count.
            source.rangeEquals(0, "A".encodeUtf8(), 0, -1).shouldBeFalse()
            // Byte count longer than bytes length.
            source.rangeEquals(0, "A".encodeUtf8(), 0, 2).shouldBeFalse()
            // Bytes offset + byte count longer than bytes length.
            source.rangeEquals(0, "A".encodeUtf8(), 1, 1).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `read nio buffer`(factory: Factory) {
        val pipe = factory.pipe()
        with(pipe) {
            val expected = if (factory.isOneByteAtTime) "a" else "abcdefg"
            sink.writeUtf8("abcdefg")
            sink.emit()

            val nioByteBuffer = ByteBuffer.allocate(1024)
            val byteCount = source.read(nioByteBuffer)
            byteCount shouldBeEqualTo expected.length
            nioByteBuffer.position() shouldBeEqualTo expected.length
            nioByteBuffer.limit() shouldBeEqualTo nioByteBuffer.capacity()

            nioByteBuffer.flip()
            val data = ByteArray(expected.length)
            nioByteBuffer[data]
            data.decodeToString() shouldBeEqualTo expected
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `read large nio buffer only reads one segment`(factory: Factory) {
        val pipe = factory.pipe()
        with(pipe) {
            val expected = if (factory.isOneByteAtTime) "a" else "a".repeat(SEGMENT_SIZE)
            sink.writeUtf8("a".repeat(SEGMENT_SIZE * 4))
            sink.emit()

            val nioByteBuffer = ByteBuffer.allocate(SEGMENT_SIZE * 3)
            val byteCount = source.read(nioByteBuffer)
            byteCount shouldBeEqualTo expected.length
            nioByteBuffer.position() shouldBeEqualTo expected.length
            nioByteBuffer.limit() shouldBeEqualTo nioByteBuffer.capacity()

            nioByteBuffer.flip()
            val data = ByteArray(expected.length)
            nioByteBuffer[data]
            data.decodeToString() shouldBeEqualTo expected
        }
    }
}

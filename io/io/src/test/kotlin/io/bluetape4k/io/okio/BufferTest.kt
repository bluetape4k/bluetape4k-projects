package io.bluetape4k.io.okio

import io.bluetape4k.io.okio.TestUtil.SEGMENT_POOL_MAX_SIZE
import io.bluetape4k.io.okio.TestUtil.SEGMENT_SIZE
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.EOFException
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BufferTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    @Test
    fun `read and write utf8`() {
        val buffer = Buffer()

        buffer.writeUtf8("ab")
        buffer.size shouldBeEqualTo 2L

        buffer.writeUtf8("cdef")
        buffer.size shouldBeEqualTo 6L

        buffer.readUtf8(4) shouldBeEqualTo "abcd"
        buffer.size shouldBeEqualTo 2L

        buffer.readUtf8(2) shouldBeEqualTo "ef"
        buffer.size shouldBeEqualTo 0L

        assertFailsWith<EOFException> {
            buffer.readUtf8(1)
        }
        buffer.exhausted().shouldBeTrue()
    }

    @Test
    fun `buffer to string`() {
        Buffer().toString() shouldBeEqualTo "[size=0]"
        Buffer().writeUtf8("a\r\nb\nc\rd\\e").toString() shouldBeEqualTo "[text=a\\r\\nb\\nc\\rd\\\\e]"
        Buffer().writeUtf8("Tyrannosaur").toString() shouldBeEqualTo "[text=Tyrannosaur]"

        Buffer().write("74c999cb8872616ec999cb8c73c3b472".decodeHex()).toString() shouldBeEqualTo "[text=təˈranəˌsôr]"

        Buffer().write(ByteArray(16)).toString() shouldBeEqualTo
                "[hex=00000000000000000000000000000000]"
    }

    @Test
    fun `multiple segment buffers`() {
        val buffer = Buffer().apply {
            writeUtf8("a".repeat(1_000))
            writeUtf8("b".repeat(2_500))
            writeUtf8("c".repeat(5_000))
            writeUtf8("d".repeat(10_000))
            writeUtf8("e".repeat(25_000))
            writeUtf8("f".repeat(50_000))
        }

        buffer.readUtf8(999) shouldBeEqualTo "a".repeat(999)  // a...a
        buffer.readUtf8(2502) shouldBeEqualTo "a" + "b".repeat(2500) + "c"  // ab...bc
        buffer.readUtf8(4998) shouldBeEqualTo "c".repeat(4998)  // c...c
        buffer.readUtf8(10002) shouldBeEqualTo "c" + "d".repeat(10000) + "e"  // cd...de
        buffer.readUtf8(24998) shouldBeEqualTo "e".repeat(24998)  // e...e
        buffer.readUtf8(50001) shouldBeEqualTo "e" + "f".repeat(50000)  // ef...f

        buffer.exhausted().shouldBeTrue()
    }

    @Test
    fun `fill and drain pool`() {
        val buffer = Buffer()

        // Take 2 * MAX_SIZE segments. This will drain the pool, even if other tests filled it.
        buffer.write(ByteArray(SEGMENT_POOL_MAX_SIZE))
        buffer.write(ByteArray(SEGMENT_POOL_MAX_SIZE))

        // Recycle MAX_SIZE segments. They're all in the pool.
        buffer.skip(SEGMENT_POOL_MAX_SIZE.toLong())

        // Recycle MAX_SIZE more segments. The pool is full so they get garbage collected.
        buffer.skip(SEGMENT_POOL_MAX_SIZE.toLong())

        // Take MAX_SIZE segments to drain the pool.
        buffer.write(ByteArray(SEGMENT_POOL_MAX_SIZE))

        // Take MAX_SIZE more segments. The pool is drained so these will need to be allocated.
        buffer.write(ByteArray(SEGMENT_POOL_MAX_SIZE))
    }

    @Test
    fun `move bytes between buffers share segment`() {
        val size = SEGMENT_SIZE / 2 - 1
        moveBytesBetweenBuffers("a".repeat(size), "b".repeat(size))
    }

    @Test
    fun `move bytes between buffers reassign segment`() {
        val size = SEGMENT_SIZE / 2 + 1
        moveBytesBetweenBuffers("a".repeat(size), "b".repeat(size))
    }

    @Test
    fun `move bytes between buffers multiple segments`() {
        val size = 3 * SEGMENT_SIZE + 1
        moveBytesBetweenBuffers("a".repeat(size), "b".repeat(size))
    }

    private fun moveBytesBetweenBuffers(vararg contents: String) {
        val expected = StringBuilder()
        val buffer = Buffer()

        contents.forEach { s ->
            val source = bufferOf(s)
            buffer.writeAll(source)
            expected.append(s)
        }

        buffer.readUtf8(expected.length.toLong()) shouldBeEqualTo expected.toString()
    }


    /** The big part of source's first segment is being moved.  */
    @Test
    fun `write split source buffer left`() {
        val writeSize = SEGMENT_SIZE / 2 + 1

        val sink = bufferOf("b".repeat(SEGMENT_SIZE - 10))
        val source = bufferOf("a".repeat(2 * SEGMENT_SIZE))

        log.debug { "sink size=${sink.size}, source size=${source.size}" }

        sink.write(source, writeSize.toLong())

        log.debug { "write size=$writeSize" }
        log.debug { "sink size=${sink.size}, source size=${source.size}" }

        sink.size shouldBeEqualTo SEGMENT_SIZE - 10L + writeSize
        source.size shouldBeEqualTo 2L * SEGMENT_SIZE - writeSize
    }

    @Test
    fun `copy to spanning segments`() {
        val source = Buffer().apply {
            writeUtf8("a".repeat(SEGMENT_SIZE * 2))
            writeUtf8("b".repeat(SEGMENT_SIZE * 2))
        }

        val out = ByteArrayOutputStream()
        source.copyTo(out, 10L, SEGMENT_SIZE * 3L)

        out.toString() shouldBeEqualTo
                "a".repeat(SEGMENT_SIZE * 2 - 10) + "b".repeat(SEGMENT_SIZE + 10)

        source.readUtf8(SEGMENT_SIZE * 4L) shouldBeEqualTo
                "a".repeat(SEGMENT_SIZE * 2) + "b".repeat(SEGMENT_SIZE * 2)
    }

    @Test
    fun `copy to stream`() {
        val buffer = bufferOf("hello, world!")
        val out = ByteArrayOutputStream()
        buffer.copyTo(out)

        val outString = out.toByteArray().toUtf8String()
        outString shouldBeEqualTo "hello, world!"
        buffer.readUtf8() shouldBeEqualTo "hello, world!"
    }

    @Test
    fun `write to spanning segments`() {
        val source = Buffer().apply {
            writeUtf8("a".repeat(SEGMENT_SIZE * 2))
            writeUtf8("b".repeat(SEGMENT_SIZE * 2))
        }

        val out = ByteArrayOutputStream()

        source.skip(10L)
        source.writeTo(out, SEGMENT_SIZE * 3L)

        out.toString() shouldBeEqualTo
                "a".repeat(SEGMENT_SIZE * 2 - 10) + "b".repeat(SEGMENT_SIZE + 10)

        source.readUtf8(source.size) shouldBeEqualTo "b".repeat(SEGMENT_SIZE - 10)
    }

    @Test
    fun `write to stream`() {
        val buffer = bufferOf("hello, world!")
        val out = ByteArrayOutputStream()
        buffer.writeTo(out)

        val outString = out.toByteArray().toUtf8String()
        outString shouldBeEqualTo "hello, world!"
        buffer.exhausted().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read from stream`() {
        val expectedText = Fakers.randomString(512)
        val input = ByteArrayInputStream(expectedText.toUtf8Bytes())

        val buffer = bufferOf(input)

        buffer.readUtf8() shouldBeEqualTo expectedText
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read from spanning segments`() {
        val expectedText = Fakers.randomString(1024)
        val input = ByteArrayInputStream(expectedText.toUtf8Bytes())
        val buffer = bufferOf("a".repeat(SEGMENT_SIZE - 10))
        buffer.readFrom(input)

        val out = buffer.readUtf8()
        out shouldBeEqualTo "a".repeat(SEGMENT_SIZE - 10) + expectedText
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read from stream with count`() {
        val expectedText = Fakers.randomString(512)
        val input = ByteArrayInputStream(expectedText.toUtf8Bytes())
        val buffer = bufferOf(input, 10)

        val out = buffer.readUtf8()
        out shouldBeEqualTo expectedText.substring(0, 10)
    }

    @Test
    fun `move all requested bytes with read`() {
        val sink = bufferOf("a".repeat(10))
        val source = bufferOf("b".repeat(15))

        // source를 읽어 sink 에 쓴다 
        source.read(sink, 10) shouldBeEqualTo 10
        sink.size shouldBeEqualTo 20
        source.size shouldBeEqualTo 5

        sink.readUtf8() shouldBeEqualTo "a".repeat(10) + "b".repeat(10)
    }

    @Test
    fun `move fewer than requested bytes with read`() {
        val sink = bufferOf("a".repeat(10))
        val source = bufferOf("b".repeat(20))

        // source를 읽어 sink 에 쓴다 (source의 전체 내용이 20바이트만 이동한다)
        source.read(sink, 25) shouldBeEqualTo 20
        sink.size shouldBeEqualTo 30
        source.size shouldBeEqualTo 0

        sink.readUtf8() shouldBeEqualTo "a".repeat(10) + "b".repeat(20)
    }

    @Test
    fun `indexOf with offset`() {
        val buffer = Buffer()
        val halfSegment: Int = SEGMENT_SIZE / 2

        buffer.writeUtf8("a".repeat(halfSegment))
        buffer.writeUtf8("b".repeat(halfSegment))
        buffer.writeUtf8("c".repeat(halfSegment))
        buffer.writeUtf8("d".repeat(halfSegment))

        buffer.indexOf('a'.code.toByte(), 0) shouldBeEqualTo 0
        buffer.indexOf('a'.code.toByte(), halfSegment - 1L) shouldBeEqualTo halfSegment - 1L
        buffer.indexOf('b'.code.toByte(), halfSegment - 1L) shouldBeEqualTo halfSegment.toLong()
        buffer.indexOf('c'.code.toByte(), halfSegment - 1L) shouldBeEqualTo halfSegment * 2L
        buffer.indexOf('d'.code.toByte(), halfSegment - 1L) shouldBeEqualTo halfSegment * 3L
        buffer.indexOf('d'.code.toByte(), halfSegment * 2L) shouldBeEqualTo halfSegment * 3L
        buffer.indexOf('d'.code.toByte(), halfSegment * 3L) shouldBeEqualTo halfSegment * 3L
        buffer.indexOf('d'.code.toByte(), halfSegment * 4L - 1L) shouldBeEqualTo halfSegment * 4L - 1L
    }

    @Test
    fun byteAt() {
        val buffer = Buffer()
        buffer.writeUtf8("a")
        buffer.writeUtf8("b".repeat(SEGMENT_SIZE))
        buffer.writeUtf8("c")

        buffer[0] shouldBeEqualTo 'a'.code.toByte()
        buffer[0] shouldBeEqualTo 'a'.code.toByte()  // getByte doesn't mutate!
        buffer[buffer.size - 1] shouldBeEqualTo 'c'.code.toByte()
        buffer[buffer.size - 2] shouldBeEqualTo 'b'.code.toByte()
        buffer[buffer.size - 3] shouldBeEqualTo 'b'.code.toByte()
    }

    @Test
    fun `get byte of empty buffer`() {
        val buffer = Buffer()
        assertFailsWith<IndexOutOfBoundsException> {
            buffer[0]
        }
    }

    @Test
    fun `write prefix to empty buffer`() {
        val sink = Buffer()
        val source = bufferOf("abcd")
        sink.write(source, 2)
        sink.readUtf8() shouldBeEqualTo "ab"
    }

    @Test
    fun `clone does not observe writes to original`() {
        val original = Buffer()
        val clone = original.clone()
        clone.exhausted().shouldBeTrue()

        original.writeUtf8("abc")
        clone.exhausted().shouldBeTrue()
    }

    @Test
    fun `original does not observe read from clone`() {
        val original = bufferOf("abc")
        val clone = original.clone()

        clone.readUtf8() shouldBeEqualTo "abc"
        original.size shouldBeEqualTo 3L
        original.readUtf8(2) shouldBeEqualTo "ab"
    }

    @Test
    fun `buffer inputstream read byte by byte`() {
        val source = bufferOf("abc")
        val input = source.inputStream()

        input.available() shouldBeEqualTo 3
        input.read() shouldBeEqualTo 'a'.code
        input.read() shouldBeEqualTo 'b'.code
        input.read() shouldBeEqualTo 'c'.code
        input.read() shouldBeEqualTo -1
        input.available() shouldBeEqualTo 0
        source.exhausted().shouldBeTrue()
    }

    @Test
    fun `buffer inputstream bulk read`() {
        val source = bufferOf("abc")
        val byteArray = ByteArray(4)
        byteArray.fill(-5)

        val input = source.inputStream()

        input.read(byteArray) shouldBeEqualTo 3
        byteArray.contentToString() shouldBeEqualTo "[97, 98, 99, -5]"

        byteArray.fill(-7)
        input.read(byteArray) shouldBeEqualTo -1
        byteArray.contentToString() shouldBeEqualTo "[-7, -7, -7, -7]"
    }

    @Test
    fun `readAll writeAll segments at once`() {
        val write1 = bufferOf(
            "",
            "a".repeat(SEGMENT_SIZE),
            "b".repeat(SEGMENT_SIZE),
            "c".repeat(SEGMENT_SIZE),
        )
        val source = bufferOf(
            "",
            "a".repeat(SEGMENT_SIZE),
            "b".repeat(SEGMENT_SIZE),
            "c".repeat(SEGMENT_SIZE),
        )
        val mockSink = MockSink()

        source.readAll(mockSink) shouldBeEqualTo SEGMENT_SIZE * 3L  // source 의 내용을 모두 읽어 mockSink 에 쓴다
        source.exhausted().shouldBeTrue()

        mockSink.assertLog("write(" + write1 + ", " + write1.size + ")")
    }

    @Test
    fun copyTo() {
        val source = bufferOf("party")
        val target = Buffer()

        source.copyTo(target, 1, 3)

        target.readUtf8() shouldBeEqualTo "art"
        source.readUtf8() shouldBeEqualTo "party"
    }

    @Test
    fun `copy to source and target can be the same`() {
        val a1 = "a".repeat(SEGMENT_SIZE)
        val b1 = "b".repeat(SEGMENT_SIZE)

        val source = bufferOf(a1, b1)
        source.copyTo(source, 0, source.size)  // 자기 자신에게 복사 (append 된다)
        source.readUtf8() shouldBeEqualTo a1 + b1 + a1 + b1
    }

    @Test
    fun `copyTo with empty source`() {
        val source = Buffer()
        val target = bufferOf("aaa")

        source.copyTo(target, 0L, 0L)

        source.readUtf8() shouldBeEqualTo ""
        target.readUtf8() shouldBeEqualTo "aaa"
    }

    @Test
    fun `copyTo with empty target`() {
        val source = bufferOf("aaa")
        val target = Buffer()

        source.copyTo(target, 0L, 3L)

        source.readUtf8() shouldBeEqualTo "aaa"
        target.readUtf8() shouldBeEqualTo "aaa"
    }

    @Test
    fun `snapshot reports accurate size`() {
        val buf = bufferOf(0, 1, 2, 3)

        val snapshot: ByteString = buf.snapshot(1)
        snapshot.size shouldBeEqualTo 1
    }
}

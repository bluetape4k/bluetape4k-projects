package io.bluetape4k.io.okio

import io.bluetape4k.apache.reflectionToString
import io.bluetape4k.io.okio.TestUtil.SEGMENT_SIZE
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class BufferCursorTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3

        private val _factories = BufferFactory.factories
        private val _buffers by lazy { _factories.map { it.newBuffer() } }
    }

    fun factories() = _factories
    fun buffers() = _buffers

    // HINT: LZ4, ZSTD 에서 실제 압축한 길이만큼만 resizeBuffer() 를 이용해서 사용할 수 있다.
    // 
    @RepeatedTest(REPEAT_SIZE)
    fun `api example`() {
        val buffer = Buffer()
        buffer.readAndWriteUnsafe().use { cursor ->
            cursor.resizeBuffer(1_000_000L)
            log.debug { "cursor start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }
            do {
                Arrays.fill(cursor.data!!, cursor.start, cursor.end, 'x'.code.toByte())
            } while (cursor.next() != -1)
            buffer.size shouldBeEqualTo 1_000_000L

            cursor.seek(3)
            cursor.data!![cursor.start] = 'o'.code.toByte()
            cursor.seek(1)
            cursor.data!![cursor.start] = 'o'.code.toByte()

            cursor.resizeBuffer(4L)
            log.debug { "cursor start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }
            buffer.size shouldBeEqualTo 4L
            cursor.offset shouldBeEqualTo 4L
        }

        log.debug { "buffer=$buffer" }
        buffer shouldBeEqualTo bufferOf("xoxo")
    }

    // HINT: LZ4, ZSTD 에서 실제 압축한 길이만큼만 resizeBuffer() 를 이용해서 사용할 수 있다.
    // 
    @RepeatedTest(REPEAT_SIZE)
    fun `api example by kotlin function`() {
        val buffer = Buffer()
        buffer.readAndWriteUnsafe().use { cursor ->
            cursor.resizeBuffer(1_000_000L)
            log.debug { "cursor start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }

            val x = 'x'.code.toByte()
            val data = cursor.data!!
            do {
                data.fill(x, cursor.start, cursor.end)
            } while (cursor.next() != -1)
            buffer.size shouldBeEqualTo 1_000_000L

            cursor.seek(3)
            data[cursor.start] = 'o'.code.toByte()
            cursor.seek(1)
            data[cursor.start] = 'o'.code.toByte()

            cursor.resizeBuffer(4)
            log.debug { "cursor start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }
            buffer.size shouldBeEqualTo 4L
            cursor.offset shouldBeEqualTo 4L
        }
        log.debug { "buffer=$buffer" }
        buffer shouldBeEqualTo bufferOf("xoxo")
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `access segment by segment`(buffer: Buffer) {
        buffer.readUnsafe().use { cursor ->
            val actual = Buffer()
            while (cursor.next() != -1) {
                log.debug { "cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }
                actual.write(cursor.data!!, cursor.start, cursor.end - cursor.start)
            }
            actual shouldBeEqualTo buffer
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `seek to negative one seeks before first segment`(buffer: Buffer) {
        buffer.readUnsafe().use { cursor ->
            log.debug { "before seek -1L, cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }
            cursor.seek(-1L)
            log.debug { "after seek -1L, cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }

            cursor.offset shouldBeEqualTo -1
            cursor.data.shouldBeNull()
            cursor.start shouldBeEqualTo -1
            cursor.end shouldBeEqualTo -1

            cursor.next()
            cursor.offset shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `access byte by byte`(buffer: Buffer) {
        buffer.readUnsafe().use { cursor ->
            val actual = ByteArray(buffer.size.toInt())
            repeat(buffer.size.toInt()) {
                cursor.seek(it.toLong())
                actual[it] = cursor.data!![cursor.start]
            }
            buffer.snapshot() shouldBeEqualTo byteStringOf(actual)
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `access byte by byte reverse`(buffer: Buffer) {
        buffer.readUnsafe().use { cursor ->
            val actual = ByteArray(buffer.size.toInt())
            for (i in (buffer.size - 1).toInt() downTo 0) {
                cursor.seek(i.toLong())
                actual[i] = cursor.data!![cursor.start]
            }
            buffer.snapshot() shouldBeEqualTo byteStringOf(actual)
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `access byte by byte always resetting to zero`(buffer: Buffer) {
        buffer.readUnsafeAndClose { cursor ->
            val actual = ByteArray(buffer.size.toInt())
            for (i in 0 until buffer.size) {
                cursor.seek(i)
                actual[i.toInt()] = cursor.data!![cursor.start]
                cursor.seek(0)  // 이렇게 하면 느려진다.
            }
            buffer.snapshot() shouldBeEqualTo byteStringOf(actual)
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `segment by segment navigation`(buffer: Buffer) {
        buffer.readUnsafeAndClose { cursor ->
            cursor.offset shouldBeEqualTo -1L

            var lastOffset = cursor.offset
            while (cursor.next() != -1) {
                cursor.offset shouldBeGreaterThan lastOffset
                lastOffset = cursor.offset
            }

            log.debug { "cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }
            cursor.offset shouldBeEqualTo buffer.size
            cursor.data.shouldBeNull()
            cursor.start shouldBeEqualTo -1
            cursor.end shouldBeEqualTo -1
        }
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `seek within segment`(factory: BufferFactory) {
        Assumptions.assumeTrue { factory == BufferFactory.SMALL_SEGMENTED_BUFFER }

        val buffer = factory.newBuffer()
        buffer.clone().readUtf8() shouldBeEqualTo "abcdefghijkl"  // abc//defg//hijkl (segment 마다 따로 저장되어 있다)

        buffer.readUnsafeAndClose { cursor ->
            cursor.seek(5L) shouldBeEqualTo 2  // 2 for 2 bytes left in the segment: "fg"
            cursor.offset shouldBeEqualTo 5L
            (cursor.end - cursor.start) shouldBeEqualTo 2
            log.debug { "cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }

            Char(cursor.data!![cursor.start - 2].toUShort()).code shouldBeEqualTo 'd'.code
            Char(cursor.data!![cursor.start - 1].toUShort()).code shouldBeEqualTo 'e'.code
            Char(cursor.data!![cursor.start].toUShort()).code shouldBeEqualTo 'f'.code
            Char(cursor.data!![cursor.start + 1].toUShort()).code shouldBeEqualTo 'g'.code
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `acquire and release`(buffer: Buffer) {
        val cursor = Buffer.UnsafeCursor()

        // Nothing initialized before acquire
        cursor.offset shouldBeEqualTo -1L
        cursor.data.shouldBeNull()
        cursor.start shouldBeEqualTo -1
        cursor.end shouldBeEqualTo -1
        log.debug { "cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }

        buffer.readUnsafe(cursor)
        cursor.close()

        log.debug { "cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }
        cursor.offset shouldBeEqualTo -1L
        cursor.data.shouldBeNull()
        cursor.start shouldBeEqualTo -1
        cursor.end shouldBeEqualTo -1
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `Cursor를 중복해서 얻으려고 하면 예외가 발생합니다`(buffer: Buffer) {
        assertFailsWith<IllegalStateException> {
            buffer.readUnsafeAndClose { cursor ->
                buffer.readUnsafe(cursor)  // 중복해서 cursor 획득하려고 하면 예외를 발생시킨다
            }
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `Cursor를 얻지 않고 해제하려고 하면 예외가 발생합니다`(buffer: Buffer) {
        val cursor = Buffer.UnsafeCursor()
        assertFailsWith<IllegalStateException> {
            cursor.close()
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `Cursor를 해제한 이후에 또 해제하려고 하면 예외가 발생합니다`(buffer: Buffer) {
        val cursor = buffer.readUnsafe()
        cursor.close()

        assertFailsWith<IllegalStateException> {
            cursor.close()
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `enlarge within segment`(buffer: Buffer) {
        val originalSize = buffer.size
        val expected = Buffer()
        buffer.clone().copyTo(expected)
        expected.writeUtf8("abc")

        log.debug { "buffer=${buffer.reflectionToString()}" }

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.resizeBuffer(originalSize + 3) shouldBeEqualTo originalSize
            log.debug { "cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }

            cursor.seek(originalSize)
            cursor.data!![cursor.start] = 'a'.code.toByte()

            cursor.seek(originalSize + 1)
            cursor.data!![cursor.start] = 'b'.code.toByte()

            cursor.seek(originalSize + 2)
            cursor.data!![cursor.start] = 'c'.code.toByte()
        }
        buffer shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `enlarge by many segment`(buffer: Buffer) {
        val originalSize = buffer.size
        val expected = Buffer()
        buffer.clone().copyTo(expected)
        expected.writeUtf8("x".repeat(1_000_000))

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.resizeBuffer(originalSize + 1_000_000L)
            cursor.seek(originalSize)
            log.debug { "cursor.start=${cursor.start}, end=${cursor.end}, offset=${cursor.offset}" }

            do {
                Arrays.fill(cursor.data!!, cursor.start, cursor.end, 'x'.code.toByte())
            } while (cursor.next() != -1)
        }
        buffer shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `resize not acquired`(buffer: Buffer) {
        val cursor = Buffer.UnsafeCursor()
        assertFailsWith<IllegalStateException> {
            cursor.resizeBuffer(10)
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `expand not acquired`(buffer: Buffer) {
        val cursor = Buffer.UnsafeCursor()
        assertFailsWith<IllegalStateException> {
            cursor.expandBuffer(10)
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `resize acquire readonly`(buffer: Buffer) {
        assertFailsWith<IllegalStateException> {
            buffer.readUnsafeAndClose { cursor ->
                cursor.resizeBuffer(10)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `expand acquired readonly`(buffer: Buffer) {
        assertFailsWith<IllegalStateException> {
            buffer.readUnsafeAndClose { cursor ->
                cursor.expandBuffer(10)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `shrink within segment`(buffer: Buffer) {
        Assumptions.assumeTrue { buffer.size > 3 }
        val originalSize = buffer.size
        val expected = Buffer()
        buffer.copyTo(expected, 0, originalSize - 3)
        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.resizeBuffer(originalSize - 3) shouldBeEqualTo originalSize
        }
        buffer shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `shink by many segments`(buffer: Buffer) {
        Assumptions.assumeTrue { buffer.size <= 1_000_000 }

        val originalSize = buffer.size
        val toShrink = bufferOf("x".repeat(1_000_000))
        buffer.copyTo(toShrink, 0, originalSize)

        val unsafeCursor = Buffer.UnsafeCursor()
        toShrink.readAndWriteUnsafeAndClose(unsafeCursor) { cursor ->
            cursor.resizeBuffer(originalSize)
            log.debug { "cursor=${cursor.reflectionToString()}" }
        }
        val expected = bufferOf("x".repeat(originalSize.toInt()))
        toShrink shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `shrink adjust offset`(buffer: Buffer) {
        Assumptions.assumeTrue { buffer.size > 4 }

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.seek(buffer.size - 1)
            cursor.resizeBuffer(3)
            log.debug { "cursor=${cursor.reflectionToString()}" }

            cursor.offset shouldBeEqualTo 3L
            cursor.data.shouldBeNull()
            cursor.start shouldBeEqualTo -1
            cursor.end shouldBeEqualTo -1
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `resize to same size seeks to end`(buffer: Buffer) {
        val originalSize = buffer.size

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.seek(buffer.size / 2)
            buffer.size shouldBeEqualTo originalSize

            cursor.resizeBuffer(originalSize)
            log.debug { "cursor=${cursor.reflectionToString()}" }

            buffer.size shouldBeEqualTo originalSize
            cursor.offset shouldBeEqualTo originalSize

            cursor.data.shouldBeNull()
            cursor.start shouldBeEqualTo -1
            cursor.end shouldBeEqualTo -1
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `resize enlarge moves cursor to old size`(buffer: Buffer) {
        val originalSize = buffer.size
        val expected = Buffer()
        buffer.clone().copyTo(expected)
        expected.writeUtf8("a")

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.seek(buffer.size / 2)
            buffer.size shouldBeEqualTo originalSize

            cursor.resizeBuffer(originalSize + 1)
            log.debug { "cursor=${cursor.reflectionToString()}" }
            cursor.offset shouldBeEqualTo originalSize

            cursor.data.shouldNotBeNull()
            cursor.start shouldNotBeEqualTo -1
            cursor.end shouldBeEqualTo (cursor.start + 1)
            cursor.data!![cursor.start] = 'a'.code.toByte()
        }
        buffer shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `resize shrink moves cursor to end`(buffer: Buffer) {
        Assumptions.assumeTrue { buffer.size > 0L }

        val originalSize = buffer.size

        log.debug { "buffer=${buffer.reflectionToString()}" }
        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.seek(buffer.size / 2)
            buffer.size shouldBeEqualTo originalSize

            cursor.resizeBuffer(originalSize - 1)
            log.debug { "cursor=${cursor.reflectionToString()}" }
            cursor.offset shouldBeEqualTo originalSize - 1

            cursor.data.shouldBeNull()
            cursor.start shouldBeEqualTo -1
            cursor.end shouldBeEqualTo -1
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun expand(buffer: Buffer) {
        val originalSize = buffer.size
        val expected = bufferOf(buffer)
        expected.writeUtf8("abcde")

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.expandBuffer(5)
            log.debug { "cursor=${cursor.reflectionToString()}" }

            for (i in 0..4) {
                cursor.data!![cursor.start + i] = ('a'.code + i).toByte()
            }
            cursor.resizeBuffer(originalSize + 5)
            log.debug { "cursor=${cursor.reflectionToString()}" }
        }
        buffer shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `expand same segment`(buffer: Buffer) {
        Assumptions.assumeTrue { buffer.size > 0 }

        val originalSize = buffer.size

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.seek(originalSize - 1)
            val originalEnd = cursor.end
            log.debug { "original end=$originalEnd" }
            Assumptions.assumeTrue { originalEnd < SEGMENT_SIZE }

            val addedByteCount = cursor.expandBuffer(1)
            addedByteCount shouldBeEqualTo SEGMENT_SIZE.toLong() - originalEnd
            buffer.size shouldBeEqualTo originalSize + addedByteCount
            cursor.offset shouldBeEqualTo originalSize
            cursor.start shouldBeEqualTo originalEnd
            cursor.end shouldBeEqualTo SEGMENT_SIZE
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `expand new segment`(buffer: Buffer) {
        val originalSize = buffer.size

        buffer.readAndWriteUnsafeAndClose { cursor ->
            val addedByteCount = cursor.expandBuffer(SEGMENT_SIZE)
            addedByteCount shouldBeEqualTo SEGMENT_SIZE.toLong()
            cursor.offset = originalSize
            cursor.start shouldBeEqualTo 0
            cursor.end shouldBeEqualTo SEGMENT_SIZE
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    fun `expand moves offset to old size`(buffer: Buffer) {
        val originalSize = buffer.size

        buffer.readAndWriteUnsafeAndClose { cursor ->
            cursor.seek(buffer.size / 2)
            log.debug { "cursor=${cursor.reflectionToString()}" }

            buffer.size shouldBeEqualTo originalSize

            val addedByteCount = cursor.expandBuffer(5)

            log.debug { "cursor=${cursor.reflectionToString()}" }
            buffer.size shouldBeEqualTo originalSize + addedByteCount
            cursor.offset shouldBeEqualTo originalSize
        }
    }
}

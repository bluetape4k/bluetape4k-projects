package io.bluetape4k.io

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.Reader
import java.io.StringReader

class BoundedLineReaderSupportTest {

    @Test
    fun `LF CRLF CR empty line and final line without newline are preserved`() {
        val bounded = StringReader("lf\n\r\ncr\rfinal")
            .boundedLineReader(maxLineChars = 8, bufferSize = 2)

        bounded.readLine() shouldBeEqualTo "lf"
        bounded.readLine() shouldBeEqualTo ""
        bounded.readLine() shouldBeEqualTo "cr"
        bounded.readLine() shouldBeEqualTo "final"
        bounded.readLine() shouldBeEqualTo null
    }

    @Test
    fun `empty input and empty lines are distinguished from EOF`() {
        StringReader("").boundedLineReader(maxLineChars = 0).readLine() shouldBeEqualTo null

        val bounded = StringReader("\n\r\n\r")
            .boundedLineReader(maxLineChars = 0, bufferSize = 1)

        bounded.readLine() shouldBeEqualTo ""
        bounded.readLine() shouldBeEqualTo ""
        bounded.readLine() shouldBeEqualTo ""
        bounded.readLine() shouldBeEqualTo null
    }

    @Test
    fun `limit boundary accepts exact length and rejects the next code unit`() {
        StringReader("ab\n").boundedLineReader(3).readLine() shouldBeEqualTo "ab"
        StringReader("abc\n").boundedLineReader(3).readLine() shouldBeEqualTo "abc"

        val failure = assertFailsWith<LineLimitExceededException> {
            StringReader("abcd\n").boundedLineReader(3).readLine()
        }

        failure.maxLineChars shouldBeEqualTo 3
        failure.message.orEmpty().contains("abcd").shouldBeEqualTo(false)
    }

    @Test
    fun `zero limit accepts only a terminated empty line`() {
        StringReader("\n").boundedLineReader(0).readLine() shouldBeEqualTo ""

        val failure = assertFailsWith<LineLimitExceededException> {
            StringReader("x").boundedLineReader(0).readLine()
        }

        failure.maxLineChars shouldBeEqualTo 0
    }

    @Test
    fun `supplementary character counts as two UTF-16 code units`() {
        StringReader("😀\n").boundedLineReader(2).readLine() shouldBeEqualTo "😀"

        val failure = assertFailsWith<LineLimitExceededException> {
            StringReader("😀\n").boundedLineReader(1).readLine()
        }

        failure.maxLineChars shouldBeEqualTo 1
    }

    @Test
    fun `CR lookahead preserves the first code unit of the next line`() {
        val reader = TrackingReader("first\rsecond")
        val bounded = reader.boundedLineReader(maxLineChars = 16, bufferSize = 2)

        bounded.readLine() shouldBeEqualTo "first"
        bounded.readLine() shouldBeEqualTo "second"
        bounded.readLine() shouldBeEqualTo null
    }

    @Test
    fun `generated reader fails after max plus one code units`() {
        val reader = GeneratedReader()
        val failure = assertFailsWith<LineLimitExceededException> {
            reader.boundedLineReader(maxLineChars = 4, bufferSize = 3).readLine()
        }

        failure.maxLineChars shouldBeEqualTo 4
        reader.charsRead shouldBeEqualTo 5
    }

    @Test
    fun `bulk read zero falls back to one code unit read`() {
        val reader = TrackingReader("x\n", bulkZeroCount = 1)
        val bounded = reader.boundedLineReader(maxLineChars = 4, bufferSize = 2)

        bounded.readLine() shouldBeEqualTo "x"
        reader.bulkReadCalls shouldBeEqualTo 2
        reader.singleReadCalls shouldBeEqualTo 1
    }

    @Test
    fun `invalid limits are rejected before touching the reader`() {
        val reader = TrackingReader("x")

        assertFailsWith<IllegalArgumentException> {
            reader.boundedLineReader(maxLineChars = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            reader.boundedLineReader(maxLineChars = 1, bufferSize = 0)
        }

        reader.bulkReadCalls shouldBeEqualTo 0
        reader.singleReadCalls shouldBeEqualTo 0
    }

    @Test
    fun `underlying reader failure keeps its identity and wrapper never closes it`() {
        val expected = IOException("read failed")
        val reader = TrackingReader("x", readFailure = expected)

        val actual = assertFailsWith<IOException> {
            reader.boundedLineReader(maxLineChars = 4).readLine()
        }

        actual shouldBeSameInstanceAs expected
        reader.closeCalls shouldBeEqualTo 0

        reader.close()
        reader.closeCalls shouldBeEqualTo 1
    }

    private class TrackingReader(
        source: String,
        bulkZeroCount: Int = 0,
        private val readFailure: IOException? = null,
    ) : Reader() {
        private val source = source.toCharArray()
        private var position = 0
        private var remainingBulkZeroCount = bulkZeroCount

        var charsRead: Int = 0
            private set
        var bulkReadCalls: Int = 0
            private set
        var singleReadCalls: Int = 0
            private set
        var closeCalls: Int = 0
            private set

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            bulkReadCalls++
            readFailure?.let { throw it }
            if (len == 0) return 0
            val result = when {
                remainingBulkZeroCount > 0 -> {
                    remainingBulkZeroCount--
                    0
                }
                position >= source.size -> -1
                else -> {
                    val count = minOf(len, source.size - position)
                    source.copyInto(cbuf, off, position, position + count)
                    position += count
                    charsRead += count
                    count
                }
            }
            return result
        }

        override fun read(): Int {
            singleReadCalls++
            readFailure?.let { throw it }
            if (position >= source.size) return -1

            charsRead++
            return source[position++].code
        }

        override fun close() {
            closeCalls++
        }
    }

    private class GeneratedReader : Reader() {
        var charsRead: Int = 0
            private set

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            require(len > 0)
            cbuf.fill('x', off, off + len)
            charsRead += len
            return len
        }

        override fun read(): Int {
            charsRead++
            return 'x'.code
        }

        override fun close() = Unit
    }
}

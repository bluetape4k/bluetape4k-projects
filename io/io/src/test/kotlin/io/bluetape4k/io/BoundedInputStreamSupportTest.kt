package io.bluetape4k.io

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InputStream

class BoundedInputStreamSupportTest {

    @Test
    fun `상한 이하에서는 전체 본문을 반환하고 stream을 닫지 않는다`() {
        listOf(3, 4).forEach { size ->
            val expected = ByteArray(size) { it.toByte() }
            val stream = TrackingInputStream(expected)

            stream.readAllBytes(maxBytes = 4) shouldBeEqualTo expected
            stream.consumedBytes shouldBeEqualTo size
            stream.closeCalls shouldBeEqualTo 0
        }
    }

    @Test
    fun `상한을 넘으면 한 byte만 미리 읽고 부분 결과 없이 실패한다`() {
        listOf(5, 8).forEach { size ->
            val stream = TrackingInputStream(ByteArray(size) { it.toByte() })

            val failure = assertFailsWith<ByteLimitExceededException> {
                stream.readAllBytes(maxBytes = 4)
            }

            failure.maxBytes shouldBeEqualTo 4
            stream.consumedBytes shouldBeEqualTo 5
            stream.bulkReadCalls shouldBeEqualTo 1
            stream.singleReadCalls shouldBeEqualTo 1
            stream.closeCalls shouldBeEqualTo 0
        }
    }

    @Test
    fun `상한이 0이면 empty만 허용한다`() {
        val empty = TrackingInputStream(byteArrayOf())
        empty.readAllBytes(maxBytes = 0).isEmpty().shouldBeTrue()
        empty.consumedBytes shouldBeEqualTo 0
        empty.closeCalls shouldBeEqualTo 0

        val nonEmpty = TrackingInputStream(byteArrayOf(1))
        val failure = assertFailsWith<ByteLimitExceededException> {
            nonEmpty.readAllBytes(maxBytes = 0)
        }
        failure.maxBytes shouldBeEqualTo 0
        nonEmpty.consumedBytes shouldBeEqualTo 1
        nonEmpty.singleReadCalls shouldBeEqualTo 1
        nonEmpty.closeCalls shouldBeEqualTo 0
    }

    @Test
    fun `음수 상한은 읽기 전에 거부한다`() {
        val stream = TrackingInputStream(byteArrayOf(1))

        assertFailsWith<IllegalArgumentException> {
            stream.readAllBytes(maxBytes = -1)
        }

        stream.bulkReadCalls shouldBeEqualTo 0
        stream.singleReadCalls shouldBeEqualTo 0
        stream.closeCalls shouldBeEqualTo 0
    }

    @Test
    fun `Int MAX VALUE 상한은 작은 본문을 큰 배열 선할당 없이 읽는다`() {
        val expected = byteArrayOf(1, 2, 3)
        val stream = TrackingInputStream(expected)

        stream.readAllBytes(maxBytes = Int.MAX_VALUE) shouldBeEqualTo expected
        stream.consumedBytes shouldBeEqualTo expected.size
        stream.bulkReadCalls shouldBeEqualTo 2
        stream.closeCalls shouldBeEqualTo 0
    }

    @Test
    fun `bulk read가 0이면 single read로 진행한다`() {
        val expected = byteArrayOf(1, 2, 3, 4)
        val stream = TrackingInputStream(expected, bulkZeroCount = 3)

        stream.readAllBytes(maxBytes = 4) shouldBeEqualTo expected
        stream.bulkReadCalls shouldBeEqualTo 4
        stream.singleReadCalls shouldBeEqualTo 4
        stream.closeCalls shouldBeEqualTo 0
    }

    @Test
    fun `read 실패는 원본 인스턴스를 보존하고 stream을 닫지 않는다`() {
        val expected = IOException("read failed")
        val stream = TrackingInputStream(byteArrayOf(1), readFailure = expected)

        val actual = assertFailsWith<IOException> {
            stream.readAllBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs expected
        stream.closeCalls shouldBeEqualTo 0
    }

    @Test
    fun `전용 예외는 상한 외에 본문 정보를 노출하지 않는다`() {
        val failure = ByteLimitExceededException(4)

        failure.maxBytes shouldBeEqualTo 4
        failure.message.orEmpty().contains("secret-body").shouldBeFalse()
        ByteLimitExceededException::class.java.methods
            .filter { it.declaringClass == ByteLimitExceededException::class.java }
            .map { it.name }
            .toSet() shouldBeEqualTo setOf("getMaxBytes")
        assertFailsWith<IllegalArgumentException> { ByteLimitExceededException(-1) }
    }

    @Test
    fun `README 공개 예제는 caller가 raw stream 수명을 소유한다`() {
        val stream = TrackingInputStream("body".toByteArray())

        stream.use { inputStream ->
            inputStream.readAllBytes(maxBytes = 64 * 1024) shouldBeEqualTo "body".toByteArray()
        }

        stream.closeCalls shouldBeEqualTo 1
    }

    private class TrackingInputStream(
        private val source: ByteArray,
        bulkZeroCount: Int = 0,
        private val readFailure: Throwable? = null,
    ): InputStream() {
        private var position = 0
        private var remainingBulkZeroCount = bulkZeroCount

        var consumedBytes: Int = 0
            private set
        var bulkReadCalls: Int = 0
            private set
        var singleReadCalls: Int = 0
            private set
        var closeCalls: Int = 0
            private set

        override fun read(): Int {
            singleReadCalls++
            readFailure?.let { throw it }
            if (position >= source.size) return -1
            consumedBytes++
            return source[position++].toInt() and 0xff
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            bulkReadCalls++
            readFailure?.let { throw it }
            if (remainingBulkZeroCount > 0) {
                remainingBulkZeroCount--
                return 0
            }
            if (position >= source.size) return -1
            val count = minOf(length, source.size - position)
            source.copyInto(buffer, offset, position, position + count)
            position += count
            consumedBytes += count
            return count
        }

        override fun close() {
            closeCalls++
        }
    }
}

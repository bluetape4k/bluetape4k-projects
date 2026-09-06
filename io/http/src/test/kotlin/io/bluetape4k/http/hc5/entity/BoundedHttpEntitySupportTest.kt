package io.bluetape4k.http.hc5.entity

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.io.ByteLimitExceededException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.message.BasicClassicHttpResponse
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class BoundedHttpEntitySupportTest {

    @Test
    fun `null entity는 empty로 정규화한다`() {
        val entity: HttpEntity? = null

        entity.readBodyBytes(maxBytes = 0).isEmpty().shouldBeTrue()
        entity.readBodyString(maxBytes = 4) shouldBeEqualTo ""
    }

    @Test
    fun `known length와 실제 본문을 모두 strict 상한으로 검증하고 stream을 닫는다`() {
        listOf(3, 4).forEach { size ->
            val stream = TrackingInputStream(ByteArray(size) { it.toByte() })
            val entity = entity(contentLength = size.toLong(), stream = stream)

            entity.readBodyBytes(maxBytes = 4) shouldBeEqualTo ByteArray(size) { it.toByte() }
            stream.closeCalls shouldBeEqualTo 1
        }

        listOf(5, 8).forEach { size ->
            val stream = TrackingInputStream(ByteArray(size) { it.toByte() })
            val entity = entity(contentLength = -1L, stream = stream)

            val failure = assertFailsWith<ByteLimitExceededException> {
                entity.readBodyBytes(maxBytes = 4)
            }
            failure.maxBytes shouldBeEqualTo 4
            stream.consumedBytes shouldBeEqualTo 5
            stream.closeCalls shouldBeEqualTo 1
        }
    }

    @Test
    fun `known length는 힌트일 뿐이며 non-null empty와 더 짧은 실제 본문을 반환한다`() {
        listOf(byteArrayOf(), byteArrayOf(1, 2)).forEach { expected ->
            val stream = TrackingInputStream(expected)

            entity(contentLength = 4L, stream = stream).readBodyBytes(maxBytes = 4) shouldBeEqualTo expected
            stream.closeCalls shouldBeEqualTo 1
        }
    }

    @Test
    fun `known length가 상한 이하여도 실제 본문이 크면 overflow다`() {
        val stream = TrackingInputStream(ByteArray(5))

        val failure = assertFailsWith<ByteLimitExceededException> {
            entity(contentLength = 3L, stream = stream).readBodyBytes(maxBytes = 4)
        }

        failure.maxBytes shouldBeEqualTo 4
        stream.consumedBytes shouldBeEqualTo 5
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `known oversize는 content를 읽지 않고 획득한 stream을 닫는다`() {
        val stream = TrackingInputStream(ByteArray(8))
        val entity = entity(contentLength = 8L, stream = stream)

        val failure = assertFailsWith<ByteLimitExceededException> {
            entity.readBodyBytes(maxBytes = 4)
        }

        failure.maxBytes shouldBeEqualTo 4
        stream.consumedBytes shouldBeEqualTo 0
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `음수 상한은 metadata와 content accessor 전에 거부한다`() {
        val entity = mockk<HttpEntity>()
        val nullable: HttpEntity? = null

        assertFailsWith<IllegalArgumentException> { entity.readBodyBytes(maxBytes = -1) }
        assertFailsWith<IllegalArgumentException> { nullable.readBodyBytes(maxBytes = -1) }

        verify(exactly = 0) { entity.contentLength }
        verify(exactly = 0) { entity.content }
    }

    @Test
    fun `known oversize는 accessor 실패를 suppressed로 보존한다`() {
        val accessorFailure = IOException("content accessor failed")
        val entity = mockk<HttpEntity>()
        every { entity.contentLength } returns 8L
        every { entity.content } throws accessorFailure

        val failure = assertFailsWith<ByteLimitExceededException> {
            entity.readBodyBytes(maxBytes = 4)
        }

        failure.suppressed.single() shouldBeSameInstanceAs accessorFailure
    }

    @Test
    fun `known oversize accessor cancellation은 overflow primary에 suppressed로 보존한다`() {
        val cancellation = CancellationException("cancelled")
        val entity = mockk<HttpEntity>()
        every { entity.contentLength } returns 8L
        every { entity.content } throws cancellation

        val actual = assertFailsWith<ByteLimitExceededException> {
            entity.readBodyBytes(maxBytes = 4)
        }

        actual.maxBytes shouldBeEqualTo 4
        actual.suppressed.single() shouldBeSameInstanceAs cancellation
    }

    @Test
    fun `read와 close 실패의 primary와 suppressed identity를 보존한다`() {
        val readFailure = IOException("read failed")
        val closeFailure = IOException("close failed")
        val stream = TrackingInputStream(byteArrayOf(1), readFailure = readFailure, closeFailure = closeFailure)

        val actual = assertFailsWith<IOException> {
            entity(contentLength = -1L, stream = stream).readBodyBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs readFailure
        actual.suppressed.single() shouldBeSameInstanceAs closeFailure
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `overflow와 close 실패는 overflow를 primary로 보존한다`() {
        val closeFailure = IOException("close failed")
        val stream = TrackingInputStream(ByteArray(5), closeFailure = closeFailure)

        val actual = assertFailsWith<ByteLimitExceededException> {
            entity(contentLength = -1L, stream = stream).readBodyBytes(maxBytes = 4)
        }

        actual.maxBytes shouldBeEqualTo 4
        actual.suppressed.single() shouldBeSameInstanceAs closeFailure
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `close fatal error도 원래 read 실패에 suppressed로 보존한다`() {
        val readFailure = IOException("read failed")
        val fatal = LinkageError("fatal close")
        val stream = TrackingInputStream(byteArrayOf(1), readFailure = readFailure, closeFailure = fatal)

        val actual = assertFailsWith<IOException> {
            entity(contentLength = -1L, stream = stream).readBodyBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs readFailure
        actual.suppressed.single() shouldBeSameInstanceAs fatal
    }

    @Test
    fun `성공 뒤 close 실패는 결과 대신 원본 close 실패를 던진다`() {
        val closeFailure = IOException("close failed")
        val stream = TrackingInputStream(byteArrayOf(1), closeFailure = closeFailure)

        val actual = assertFailsWith<IOException> {
            entity(contentLength = 1L, stream = stream).readBodyBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs closeFailure
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `read와 close가 같은 실패를 던져도 self suppression하지 않는다`() {
        val shared = IOException("shared failure")
        val stream = TrackingInputStream(byteArrayOf(1), readFailure = shared, closeFailure = shared)

        val actual = assertFailsWith<IOException> {
            entity(contentLength = -1L, stream = stream).readBodyBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs shared
        actual.suppressed.isEmpty().shouldBeTrue()
    }

    @Test
    fun `문자열 상한은 character가 아니라 UTF-8 byte에 적용한다`() {
        assertFailsWith<ByteLimitExceededException> {
            entity(contentLength = -1L, stream = TrackingInputStream("가나".toByteArray()))
                .readBodyString(maxBytes = 5)
        }

        entity(contentLength = -1L, stream = TrackingInputStream("가나".toByteArray()))
            .readBodyString(maxBytes = 6) shouldBeEqualTo "가나"
    }

    @Test
    fun `known oversize accessor block은 supervisor abort 후 bounded하게 종료한다`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val timeout = SocketTimeoutException("accessor timeout")
        val entity = mockk<HttpEntity>()
        every { entity.contentLength } returns 8L
        every { entity.content } answers {
            entered.countDown()
            release.await()
            throw timeout
        }

        withVirtualExecutor { executor ->
            val future = executor.submit<ByteArray> { entity.readBodyBytes(maxBytes = 4) }
            try {
                entered.await(5, SECONDS).shouldBeTrue()
                release.countDown()
                val wrapper = assertFailsWith<ExecutionException> { future.get(5, SECONDS) }
                val failure = wrapper.cause as ByteLimitExceededException
                failure.suppressed.single() shouldBeSameInstanceAs timeout
            } finally {
                release.countDown()
                future.cancel(true)
            }
        }
    }

    @Test
    fun `known oversize close block은 supervisor abort 후 bounded하게 종료한다`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val stream = object: TrackingInputStream(ByteArray(8)) {
            override fun close() {
                closeCalls++
                entered.countDown()
                release.await()
            }
        }

        withVirtualExecutor { executor ->
            val future = executor.submit<ByteArray> {
                entity(contentLength = 8L, stream = stream).readBodyBytes(maxBytes = 4)
            }
            try {
                entered.await(5, SECONDS).shouldBeTrue()
                stream.consumedBytes shouldBeEqualTo 0
                release.countDown()
                val wrapper = assertFailsWith<ExecutionException> { future.get(5, SECONDS) }
                (wrapper.cause as ByteLimitExceededException).maxBytes shouldBeEqualTo 4
                stream.closeCalls shouldBeEqualTo 1
            } finally {
                release.countDown()
                future.cancel(true)
            }
        }
    }

    @Test
    fun `기존 API는 strict failure가 아니라 prefix truncation을 유지한다`() {
        StringEntity("12345", ContentType.TEXT_PLAIN).toByteArrayOrNull(maxResultLength = 4)!!
            .toString(Charsets.UTF_8) shouldBeEqualTo "1234"
        StringEntity("12345", ContentType.TEXT_PLAIN)
            .toStringOrNull(maxResultLength = 4) shouldBeEqualTo "1234"
    }

    @Test
    fun `README 공개 예제는 response와 entity content 수명을 구분한다`() {
        val stream = TrackingInputStream("body".toByteArray())
        val responseEntity = entity(contentLength = 4L, stream = stream)
        val response = BasicClassicHttpResponse(200).apply {
            entity = responseEntity
        }

        response.use {
            it.entity.readBodyBytes(maxBytes = 64 * 1024) shouldBeEqualTo "body".toByteArray()
        }

        stream.closeCalls shouldBeEqualTo 1
        verify(exactly = 1) { responseEntity.close() }
    }

    private fun entity(contentLength: Long, stream: InputStream): HttpEntity =
        mockk<HttpEntity>(relaxed = true).also { entity ->
            every { entity.contentLength } returns contentLength
            every { entity.content } returns stream
        }

    private inline fun <T> withVirtualExecutor(block: (java.util.concurrent.ExecutorService) -> T): T {
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
            return block(executor)
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(5, SECONDS).shouldBeTrue()
        }
    }

    private open class TrackingInputStream(
        private val source: ByteArray,
        private val readFailure: Throwable? = null,
        private val closeFailure: Throwable? = null,
    ): InputStream() {
        private var position = 0

        var consumedBytes: Int = 0
            private set
        var closeCalls: Int = 0
            protected set

        override fun read(): Int {
            readFailure?.let { throw it }
            if (position >= source.size) return -1
            consumedBytes++
            return source[position++].toInt() and 0xff
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            readFailure?.let { throw it }
            if (position >= source.size) return -1
            val count = minOf(length, source.size - position)
            source.copyInto(buffer, offset, position, position + count)
            position += count
            consumedBytes += count
            return count
        }

        override fun close() {
            closeCalls++
            closeFailure?.let { throw it }
        }
    }
}

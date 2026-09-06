package io.bluetape4k.http.jdk

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.io.ByteLimitExceededException
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import javax.net.ssl.SSLSession

class BoundedHttpResponseSupportTest {

    @Test
    fun `Content-Length 단일 non-negative 값만 known length로 사용한다`() {
        val knownStream = TrackingInputStream(ByteArray(4) { it.toByte() })
        val knownResponse = FakeHttpResponse(
            stream = knownStream,
            headerValues = mapOf("Content-Length" to listOf("4")),
        )

        knownResponse.readBodyBytes(maxBytes = 4) shouldBeEqualTo ByteArray(4) { it.toByte() }
        knownStream.consumedBytes shouldBeEqualTo 4
        knownStream.closeCalls shouldBeEqualTo 1

        val unknownHeaders = listOf(
            emptyMap(),
            mapOf("Content-Length" to listOf("4, 4")),
            mapOf("Content-Length" to listOf("abc")),
            mapOf("Content-Length" to listOf("-1")),
            mapOf("Content-Length" to listOf("9223372036854775808")),
            mapOf("Content-Length" to listOf("4", "4")),
        )

        unknownHeaders.forEach { headers ->
            val stream = TrackingInputStream(ByteArray(5) { it.toByte() })
            val response = FakeHttpResponse(stream = stream, headerValues = headers)

            assertFailsWith<ByteLimitExceededException> {
                response.readBodyBytes(maxBytes = 4)
            }
            stream.consumedBytes shouldBeEqualTo 5
            stream.closeCalls shouldBeEqualTo 1
        }
    }

    @Test
    fun `known oversize header는 body를 읽지 않고 닫는다`() {
        listOf("5", "+5", " 5 ", Long.MAX_VALUE.toString()).forEach { value ->
            val stream = TrackingInputStream(ByteArray(8))
            val response = FakeHttpResponse(
                stream = stream,
                headerValues = mapOf("Content-Length" to listOf(value)),
            )

            val failure = assertFailsWith<ByteLimitExceededException> {
                response.readBodyBytes(maxBytes = 4)
            }

            failure.maxBytes shouldBeEqualTo 4
            stream.consumedBytes shouldBeEqualTo 0
            stream.closeCalls shouldBeEqualTo 1
        }
    }

    @Test
    fun `declared length가 작아도 실제 body가 크면 overflow다`() {
        val stream = TrackingInputStream(ByteArray(5))
        val response = FakeHttpResponse(
            stream = stream,
            headerValues = mapOf("Content-Length" to listOf("3")),
        )

        assertFailsWith<ByteLimitExceededException> { response.readBodyBytes(maxBytes = 4) }
        stream.consumedBytes shouldBeEqualTo 5
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `status code와 무관하게 body 계약을 적용한다`() {
        listOf(200, 404, 500).forEach { status ->
            val stream = TrackingInputStream("body".toByteArray())
            val response = FakeHttpResponse(stream = stream, code = status)

            response.readBodyString(maxBytes = 4) shouldBeEqualTo "body"
            stream.closeCalls shouldBeEqualTo 1
        }
    }

    @Test
    fun `음수 상한은 headers와 body accessor 전에 거부한다`() {
        val response = FakeHttpResponse(stream = TrackingInputStream(byteArrayOf(1)))

        assertFailsWith<IllegalArgumentException> { response.readBodyBytes(maxBytes = -1) }

        response.headersCalls shouldBeEqualTo 0
        response.bodyCalls shouldBeEqualTo 0
    }

    @Test
    fun `known oversize는 body accessor 실패를 suppressed로 보존한다`() {
        val accessorFailure = IOException("body accessor failed")
        val response = FakeHttpResponse(
            stream = TrackingInputStream(ByteArray(8)),
            headerValues = mapOf("Content-Length" to listOf("8")),
            bodySupplier = { throw accessorFailure },
        )

        val failure = assertFailsWith<ByteLimitExceededException> {
            response.readBodyBytes(maxBytes = 4)
        }

        failure.suppressed.single() shouldBeSameInstanceAs accessorFailure
    }

    @Test
    fun `일반 읽기 경로의 body accessor 실패 identity를 보존한다`() {
        val accessorFailure = IOException("body accessor failed")
        val stream = TrackingInputStream(ByteArray(4))
        val response = FakeHttpResponse(
            stream = stream,
            bodySupplier = { throw accessorFailure },
        )

        val actual = assertFailsWith<IOException> {
            response.readBodyBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs accessorFailure
        stream.consumedBytes shouldBeEqualTo 0
        stream.closeCalls shouldBeEqualTo 0
    }

    @Test
    fun `read와 close 실패의 identity를 보존한다`() {
        val readFailure = IOException("read failed")
        val closeFailure = IOException("close failed")
        val stream = TrackingInputStream(byteArrayOf(1), readFailure = readFailure, closeFailure = closeFailure)

        val actual = assertFailsWith<IOException> {
            FakeHttpResponse(stream).readBodyBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs readFailure
        actual.suppressed.single() shouldBeSameInstanceAs closeFailure
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `성공 뒤 close 실패는 결과 대신 원본 close 실패를 던진다`() {
        val closeFailure = IOException("close failed")
        val stream = TrackingInputStream(byteArrayOf(1), closeFailure = closeFailure)

        val actual = assertFailsWith<IOException> {
            FakeHttpResponse(stream).readBodyBytes(maxBytes = 4)
        }

        actual shouldBeSameInstanceAs closeFailure
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `overflow와 close 실패는 overflow를 primary로 보존한다`() {
        val closeFailure = IOException("close failed")
        val stream = TrackingInputStream(ByteArray(5), closeFailure = closeFailure)

        val actual = assertFailsWith<ByteLimitExceededException> {
            FakeHttpResponse(stream).readBodyBytes(maxBytes = 4)
        }

        actual.maxBytes shouldBeEqualTo 4
        actual.suppressed.single() shouldBeSameInstanceAs closeFailure
        stream.closeCalls shouldBeEqualTo 1
    }

    @Test
    fun `UTF-8 문자열도 byte 상한으로 판정한다`() {
        assertFailsWith<ByteLimitExceededException> {
            FakeHttpResponse(TrackingInputStream("가나".toByteArray())).readBodyString(maxBytes = 5)
        }

        FakeHttpResponse(TrackingInputStream("가나".toByteArray()))
            .readBodyString(maxBytes = 6) shouldBeEqualTo "가나"
    }

    @Test
    fun `known oversize body accessor block은 abort 후 bounded하게 종료한다`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val timeout = IOException("body timeout")
        val response = FakeHttpResponse(
            stream = TrackingInputStream(ByteArray(8)),
            headerValues = mapOf("Content-Length" to listOf("8")),
            bodySupplier = {
                entered.countDown()
                release.await()
                throw timeout
            },
        )

        withVirtualExecutor { executor ->
            val future = executor.submit<ByteArray> { response.readBodyBytes(maxBytes = 4) }
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
    fun `known oversize close block은 abort 후 bounded하게 종료한다`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val stream = object: TrackingInputStream(ByteArray(8)) {
            override fun close() {
                closeCalls++
                entered.countDown()
                release.await()
            }
        }
        val response = FakeHttpResponse(
            stream = stream,
            headerValues = mapOf("Content-Length" to listOf("8")),
        )

        withVirtualExecutor { executor ->
            val future = executor.submit<ByteArray> { response.readBodyBytes(maxBytes = 4) }
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
    fun `정확히 상한 뒤 blocking read-ahead는 외부 close로 회수한다`() {
        val enteredReadAhead = CountDownLatch(1)
        val released = CountDownLatch(1)
        val readFailure = IOException("supervisor closed stream")
        val stream = BlockingReadAheadInputStream(
            source = ByteArray(4),
            enteredReadAhead = enteredReadAhead,
            released = released,
            readFailure = readFailure,
        )
        val response = FakeHttpResponse(stream)

        withVirtualExecutor { executor ->
            val future = executor.submit<ByteArray> { response.readBodyBytes(maxBytes = 4) }
            try {
                enteredReadAhead.await(5, SECONDS).shouldBeTrue()
                stream.close()
                val wrapper = assertFailsWith<ExecutionException> { future.get(5, SECONDS) }
                wrapper.cause shouldBeSameInstanceAs readFailure
                stream.closeCalls shouldBeEqualTo 2
            } finally {
                stream.abort()
                future.cancel(true)
            }
        }
    }

    @Test
    fun `README 공개 예제는 BodyHandlers ofInputStream 응답 타입에서 컴파일된다`() {
        val response: HttpResponse<InputStream> = FakeHttpResponse(TrackingInputStream("body".toByteArray()))

        response.readBodyBytes(maxBytes = 64 * 1024) shouldBeEqualTo "body".toByteArray()
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

    private class FakeHttpResponse(
        private val stream: InputStream,
        headerValues: Map<String, List<String>> = emptyMap(),
        private val code: Int = 200,
        private val bodySupplier: () -> InputStream = { stream },
    ): HttpResponse<InputStream> {
        private val responseHeaders = HttpHeaders.of(headerValues) { _, _ -> true }
        private val responseRequest = HttpRequest.newBuilder(URI.create("https://example.com")).build()

        var headersCalls: Int = 0
            private set
        var bodyCalls: Int = 0
            private set

        override fun statusCode(): Int = code

        override fun request(): HttpRequest = responseRequest

        override fun previousResponse(): Optional<HttpResponse<InputStream>> = Optional.empty()

        override fun headers(): HttpHeaders {
            headersCalls++
            return responseHeaders
        }

        override fun body(): InputStream {
            bodyCalls++
            return bodySupplier()
        }

        override fun sslSession(): Optional<SSLSession> = Optional.empty()

        override fun uri(): URI = responseRequest.uri()

        override fun version(): HttpClient.Version = HttpClient.Version.HTTP_2
    }

    private open class TrackingInputStream(
        private val source: ByteArray,
        private val readFailure: Throwable? = null,
        private val closeFailure: Throwable? = null,
    ): InputStream() {
        private var position = 0

        var consumedBytes: Int = 0
            protected set
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

    private class BlockingReadAheadInputStream(
        source: ByteArray,
        private val enteredReadAhead: CountDownLatch,
        private val released: CountDownLatch,
        private val readFailure: IOException,
    ): TrackingInputStream(source) {
        override fun read(): Int {
            enteredReadAhead.countDown()
            released.await()
            throw readFailure
        }

        override fun close() {
            closeCalls++
            released.countDown()
        }

        fun abort() {
            released.countDown()
        }
    }
}

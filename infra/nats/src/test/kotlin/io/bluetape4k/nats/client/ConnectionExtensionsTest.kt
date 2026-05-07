package io.bluetape4k.nats.client

import io.bluetape4k.support.toUtf8Bytes
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.impl.Headers
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class ConnectionExtensionsTest {

    private lateinit var nc: Connection

    @BeforeEach
    fun setUp() {
        nc = mockk<Connection>(relaxed = true)
    }

    @Test
    fun `publish with subject and body calls underlying publish`() {
        nc.publish("test.subject", "hello")

        verify { nc.publish("test.subject", null as Headers?, "hello".toUtf8Bytes()) }
    }

    @Test
    fun `publish with subject and body and headers passes headers`() {
        val headers = Headers()
        nc.publish("test.subject", "hello", headers)

        verify { nc.publish("test.subject", headers, "hello".toUtf8Bytes()) }
    }

    @Test
    fun `publish with blank subject throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            nc.publish("", "hello")
        }
    }

    @Test
    fun `publish with subject replyTo and body calls 4-arg publish`() {
        nc.publish("test.subject", "reply.inbox", "hello")

        verify { nc.publish("test.subject", "reply.inbox", null as Headers?, "hello".toUtf8Bytes()) }
    }

    @Test
    fun `publish with blank replyTo throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            nc.publish("test.subject", "", "hello")
        }
    }

    @Test
    fun `request with subject and body returns response message`() {
        val response = mockk<Message>()
        every { nc.request(any<String>(), any(), any(), any()) } returns response

        val result = nc.request("test.subject", "body", timeout = 1.seconds)

        result shouldBeEqualTo response
    }

    @Test
    fun `request with blank subject throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            nc.request("", "body", timeout = 1.seconds)
        }
    }

    @Test
    fun `requestAsync without timeout calls request returning CompletableFuture`() {
        val response = mockk<Message>()
        val future = CompletableFuture.completedFuture(response)
        every { nc.request(any<String>(), any(), any<ByteArray>()) } returns future

        val result = nc.requestAsync("test.subject", "body")

        result shouldBeEqualTo future
    }

    @Test
    fun `requestAsync with timeout calls requestWithTimeout`() {
        val response = mockk<Message>()
        val future = CompletableFuture.completedFuture(response)
        every { nc.requestWithTimeout(any<String>(), any(), any<ByteArray>(), any()) } returns future

        val result = nc.requestAsync("test.subject", "body", timeout = 1.seconds)

        result shouldBeEqualTo future
    }

    @Test
    fun `requestAsync with message and no timeout calls request`() {
        val message = mockk<Message>()
        val response = mockk<Message>()
        val future = CompletableFuture.completedFuture(response)
        every { nc.request(message) } returns future

        val result = nc.requestAsync(message)

        result shouldBeEqualTo future
    }

    @Test
    fun `requestAsync with message and timeout calls requestWithTimeout`() {
        val message = mockk<Message>()
        val response = mockk<Message>()
        val future = CompletableFuture.completedFuture(response)
        every { nc.requestWithTimeout(message, any()) } returns future

        val result = nc.requestAsync(message, timeout = 1.seconds)

        result shouldBeEqualTo future
    }

    @Test
    fun `flush with kotlin Duration calls flush with java Duration`() {
        nc.flush(1.seconds)

        verify { nc.flush(1.seconds.toJavaDuration()) }
    }

    @Test
    fun `requestSuspending with message and no timeout awaits future`() = runTest {
        val message = mockk<Message>()
        val response = mockk<Message>()
        every { nc.request(message) } returns CompletableFuture.completedFuture(response)

        val result = nc.requestSuspending(message)

        result shouldBeEqualTo response
    }

    @Test
    fun `requestSuspending with message and timeout awaits requestWithTimeout future`() = runTest {
        val message = mockk<Message>()
        val response = mockk<Message>()
        every { nc.requestWithTimeout(message, any()) } returns CompletableFuture.completedFuture(response)

        val result = nc.requestSuspending(message, 1.seconds)

        result shouldBeEqualTo response
    }

    @Test
    fun `requestSuspending with subject and bytes awaits future`() = runTest {
        val response = mockk<Message>()
        every { nc.request(any<String>(), any(), any<ByteArray>()) } returns CompletableFuture.completedFuture(response)

        val result = nc.requestSuspending("test.subject", "hello".toUtf8Bytes())

        result shouldBeEqualTo response
    }

    @Test
    fun `requestWithTimeoutSuspending without timeout uses request path`() = runTest {
        val response = mockk<Message>()
        every { nc.request(any<String>(), any(), any<ByteArray>()) } returns CompletableFuture.completedFuture(response)

        val result = nc.requestWithTimeoutSuspending("test.subject", "hello".toUtf8Bytes())

        result shouldBeEqualTo response
    }

    @Test
    fun `requestWithTimeoutSuspending with timeout uses requestWithTimeout path`() = runTest {
        val response = mockk<Message>()
        every { nc.requestWithTimeout(any<String>(), any(), any<ByteArray>(), any()) } returns CompletableFuture.completedFuture(response)

        val result = nc.requestWithTimeoutSuspending("test.subject", "hello".toUtf8Bytes(), timeout = 1.seconds)

        result shouldBeEqualTo response
    }

    @Test
    fun `drainSuspending with kotlin Duration awaits drain future`() = runTest {
        every { nc.drain(any()) } returns CompletableFuture.completedFuture(true)

        val result = nc.drainSuspending(1.seconds)

        result.shouldBeTrue()
    }
}

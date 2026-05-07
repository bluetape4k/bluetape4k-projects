package io.bluetape4k.nats.client

import io.bluetape4k.support.toUtf8Bytes
import io.mockk.every
import io.mockk.mockk
import io.nats.client.Message
import io.nats.client.impl.Headers
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NatsMessageTest {

    @Test
    fun `natsMessage with builder creates NatsMessage`() {
        val msg = natsMessage {
            subject("foo")
            data("hello")
        }

        msg.shouldNotBeNull()
        msg.subject shouldBeEqualTo "foo"
        String(msg.data) shouldBeEqualTo "hello"
    }

    @Test
    fun `natsMessageOf wraps existing Message`() {
        val original = mockk<Message>(relaxed = true)
        every { original.subject } returns "wrapped.subject"

        val wrapped = natsMessageOf(original)

        wrapped.shouldNotBeNull()
    }

    @Test
    fun `natsMessageOf with ByteArray creates message with subject and data`() {
        val data = "hello".toUtf8Bytes()
        val msg = natsMessageOf("test.subject", data)

        msg.shouldNotBeNull()
        msg.subject shouldBeEqualTo "test.subject"
        msg.data shouldBeEqualTo data
    }

    @Test
    fun `natsMessageOf with ByteArray and replyTo sets replyTo`() {
        val data = "hello".toUtf8Bytes()
        val msg = natsMessageOf("test.subject", data, replyTo = "reply.inbox")

        msg.shouldNotBeNull()
        msg.replyTo shouldBeEqualTo "reply.inbox"
    }

    @Test
    fun `natsMessageOf with ByteArray and headers sets headers`() {
        val headers = Headers()
        headers.add("X-Test", "value")
        val msg = natsMessageOf("test.subject", "data".toUtf8Bytes(), headers = headers)

        msg.shouldNotBeNull()
        msg.headers shouldBeEqualTo headers
    }

    @Test
    fun `natsMessageOf with blank subject throws IllegalArgumentException for ByteArray`() {
        assertThrows(IllegalArgumentException::class.java) {
            natsMessageOf("", "data".toUtf8Bytes())
        }
    }

    @Test
    fun `natsMessageOf with String creates message`() {
        val msg = natsMessageOf("test.subject", "hello world")

        msg.shouldNotBeNull()
        msg.subject shouldBeEqualTo "test.subject"
        String(msg.data) shouldBeEqualTo "hello world"
    }

    @Test
    fun `natsMessageOf with String and replyTo sets replyTo`() {
        val msg = natsMessageOf("test.subject", "data", replyTo = "reply.inbox")

        msg.shouldNotBeNull()
        msg.replyTo shouldBeEqualTo "reply.inbox"
    }

    @Test
    fun `natsMessageOf with blank subject throws IllegalArgumentException for String`() {
        assertThrows(IllegalArgumentException::class.java) {
            natsMessageOf("", "data")
        }
    }

    @Test
    fun `natsMessageOf with null ByteArray creates message without data`() {
        val msg = natsMessageOf("test.subject", null as ByteArray?)

        msg.shouldNotBeNull()
        msg.subject shouldBeEqualTo "test.subject"
    }

    @Test
    fun `natsMessageOf with null String creates message without data`() {
        val msg = natsMessageOf("test.subject", null as String?)

        msg.shouldNotBeNull()
        msg.subject shouldBeEqualTo "test.subject"
    }
}

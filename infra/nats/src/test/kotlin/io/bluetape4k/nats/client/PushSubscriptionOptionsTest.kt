package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test

class PushSubscriptionOptionsTest {

    @Test
    fun `pushSubscriptionOptions with builder creates instance`() {
        val opts = pushSubscriptionOptions {
            stream("orders")
        }

        opts.shouldNotBeNull()
    }

    @Test
    fun `pushSubscriptionOf with valid stream creates instance`() {
        val opts = pushSubscriptionOf("orders")

        opts.shouldNotBeNull()
    }

    @Test
    fun `pushSubscriptionOf with blank stream throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            pushSubscriptionOf("")
        }
    }

    @Test
    fun `pushSubscriptionOf with stream and durable creates bound instance`() {
        val opts = pushSubscriptionOf("orders", "consumer-a")

        opts.shouldNotBeNull()
    }

    @Test
    fun `pushSubscriptionOf with blank durable throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            pushSubscriptionOf("orders", "")
        }
    }

    @Test
    fun `pushSubscriptionOf with blank stream and durable throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            pushSubscriptionOf("", "consumer-a")
        }
    }
}

package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Assertions.assertThrows
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
        assertThrows(IllegalArgumentException::class.java) {
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
        assertThrows(IllegalArgumentException::class.java) {
            pushSubscriptionOf("orders", "")
        }
    }

    @Test
    fun `pushSubscriptionOf with blank stream and durable throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            pushSubscriptionOf("", "consumer-a")
        }
    }
}

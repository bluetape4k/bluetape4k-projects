package io.bluetape4k.nats.client

import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PullSubscriptionOptionsTest {

    @Test
    fun `pullSubscriptionOptions with builder creates instance`() {
        val opts = pullSubscriptionOptions {
            stream("orders")
        }

        opts.shouldNotBeNull()
    }

    @Test
    fun `pullSubscriptionOptionsOf with valid stream and bind creates instance`() {
        val opts = pullSubscriptionOptionsOf("orders", "consumer-a")

        opts.shouldNotBeNull()
    }

    @Test
    fun `pullSubscriptionOptionsOf with blank stream throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            pullSubscriptionOptionsOf("", "consumer-a")
        }
    }

    @Test
    fun `pullSubscriptionOptionsOf with blank bind throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            pullSubscriptionOptionsOf("orders", "")
        }
    }
}

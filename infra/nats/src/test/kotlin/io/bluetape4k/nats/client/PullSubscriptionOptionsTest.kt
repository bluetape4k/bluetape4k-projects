package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.assertFailsWith
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
        assertFailsWith<IllegalArgumentException> {
            pullSubscriptionOptionsOf("", "consumer-a")
        }
    }

    @Test
    fun `pullSubscriptionOptionsOf with blank bind throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            pullSubscriptionOptionsOf("orders", "")
        }
    }
}

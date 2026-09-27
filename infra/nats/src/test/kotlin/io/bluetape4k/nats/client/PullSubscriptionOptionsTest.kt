package io.bluetape4k.nats.client

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import org.junit.jupiter.api.Test

class PullSubscriptionOptionsTest: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `pullSubscriptionOptions with builder creates instance`() {
        val opts = pullSubscriptionOptions {
            stream("orders")
        }
        log.debug { "opts: $opts" }
        opts.shouldNotBeNull()
    }

    @Test
    fun `pullSubscriptionOptionsOf with valid stream and bind creates instance`() {
        val opts = pullSubscriptionOptionsOf("orders", "consumer-a")

        log.debug { "opts: $opts" }
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

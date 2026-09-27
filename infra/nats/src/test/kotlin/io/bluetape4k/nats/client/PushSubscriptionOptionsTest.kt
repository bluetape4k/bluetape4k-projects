package io.bluetape4k.nats.client

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import org.junit.jupiter.api.Test

class PushSubscriptionOptionsTest: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `pushSubscriptionOptions with builder creates instance`() {
        val opts = pushSubscriptionOptions {
            stream("orders")
        }
        log.debug { "opts: $opts" }
        opts.shouldNotBeNull()
    }

    @Test
    fun `pushSubscriptionOf with valid stream creates instance`() {
        val opts = pushSubscriptionOf("orders")
        log.debug { "opts: $opts" }
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
        log.debug { "opts: $opts" }
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

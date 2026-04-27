package io.bluetape4k.nats.client

import io.nats.client.KeyValueOptions
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class KeyValueOptionsTest {

    @Test
    fun `keyValueOptions with empty builder creates instance`() {
        val kvo = keyValueOptions {}

        kvo.shouldNotBeNull()
    }

    @Test
    fun `keyValueOptions based on existing KeyValueOptions`() {
        val base = keyValueOptions {}
        val kvo = keyValueOptions(base) {}

        kvo.shouldNotBeNull()
    }

    @Test
    fun `keyValueOptions with JetStreamOptions`() {
        val jso = jetStreamOptionsOf()
        val kvo = keyValueOptions(jso) {}

        kvo.shouldNotBeNull()
    }

    @Test
    fun `keyValueOptions with JetStreamOptions and additional builder`() {
        val jso = jetStreamOptionsOf(publishNoAck = false)
        val kvo = keyValueOptions(jso) {
            // additional configuration
        }

        kvo.shouldNotBeNull()
    }
}

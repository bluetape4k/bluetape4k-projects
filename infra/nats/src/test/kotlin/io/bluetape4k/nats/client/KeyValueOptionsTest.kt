package io.bluetape4k.nats.client

import io.nats.client.KeyValueOptions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class KeyValueOptionsTest {

    @Test
    fun `keyValueOptions with empty builder creates instance`() {
        val kvo = keyValueOptions {}

        kvo.shouldNotBeNull()
    }

    @Test
    fun `keyValueOptions based on existing KeyValueOptions preserves JetStreamOptions`() {
        val jso = jetStreamOptionsOf(prefix = "base-prefix")
        val base = keyValueOptions(jso) {}
        val kvo = keyValueOptions(base) {}

        kvo.shouldNotBeNull()
        kvo.jetStreamOptions.prefix shouldBeEqualTo "base-prefix."
    }

    @Test
    fun `keyValueOptions with JetStreamOptions propagates JetStreamOptions`() {
        val jso = jetStreamOptionsOf(prefix = "kv-prefix")
        val kvo = keyValueOptions(jso) {}

        kvo.shouldNotBeNull()
        kvo.jetStreamOptions.prefix shouldBeEqualTo "kv-prefix."
    }

    @Test
    fun `keyValueOptions with JetStreamOptions and builder propagates options`() {
        val jso = jetStreamOptionsOf(publishNoAck = false)
        val kvo = keyValueOptions(jso) {}

        kvo.shouldNotBeNull()
        kvo.jetStreamOptions.isPublishNoAck shouldBeEqualTo false
    }
}

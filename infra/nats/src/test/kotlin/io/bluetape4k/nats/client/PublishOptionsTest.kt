package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.Properties

class PublishOptionsTest {

    @Test
    fun `publishOptions with builder creates instance with stream`() {
        val opts = publishOptions {
            stream("orders")
        }

        opts.shouldNotBeNull()
        opts.stream shouldBeEqualTo "orders"
    }

    @Test
    fun `publishOptionsOf with properties creates instance`() {
        val props = Properties()
        val opts = publishOptionsOf(props)

        opts.shouldNotBeNull()
    }

    @Test
    fun `publishOptionsOf with properties and builder applies stream`() {
        val props = Properties()
        val opts = publishOptionsOf(props) {
            stream("events")
        }

        opts.shouldNotBeNull()
        opts.stream shouldBeEqualTo "events"
    }
}

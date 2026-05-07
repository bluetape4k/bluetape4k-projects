package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.Properties

class PublishOptionsTest {

    @Test
    fun `publishOptions with builder creates instance with expectedStream`() {
        val opts = publishOptions {
            expectedStream("orders")
        }

        opts.shouldNotBeNull()
        opts.expectedStream shouldBeEqualTo "orders"
    }

    @Test
    fun `publishOptionsOf with properties creates instance`() {
        val props = Properties()
        val opts = publishOptionsOf(props)

        opts.shouldNotBeNull()
    }

    @Test
    fun `publishOptionsOf with properties and builder applies expectedStream`() {
        val props = Properties()
        val opts = publishOptionsOf(props) {
            expectedStream("events")
        }

        opts.shouldNotBeNull()
        opts.expectedStream shouldBeEqualTo "events"
    }
}

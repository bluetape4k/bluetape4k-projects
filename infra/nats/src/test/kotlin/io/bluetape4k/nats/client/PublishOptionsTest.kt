package io.bluetape4k.nats.client

import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.Properties

class PublishOptionsTest {

    @Test
    fun `publishOptions with builder creates instance`() {
        val opts = publishOptions {
            stream("orders")
        }

        opts.shouldNotBeNull()
        opts.stream.shouldNotBeNull()
    }

    @Test
    fun `publishOptionsOf with properties creates instance`() {
        val props = Properties()
        val opts = publishOptionsOf(props)

        opts.shouldNotBeNull()
    }

    @Test
    fun `publishOptionsOf with properties and builder applies builder`() {
        val props = Properties()
        val opts = publishOptionsOf(props) {
            stream("events")
        }

        opts.shouldNotBeNull()
    }
}

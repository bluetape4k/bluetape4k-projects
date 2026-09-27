package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import org.junit.jupiter.api.Test
import java.util.*

class PublishOptionsTest: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `publishOptions with builder creates instance with expectedStream`() {
        val opts = publishOptions {
            expectedStream("orders")
        }

        log.debug { "opts: $opts" }
        opts.shouldNotBeNull()
        opts.expectedStream shouldBeEqualTo "orders"
    }

    @Test
    fun `publishOptionsOf with properties creates instance`() {
        val props = Properties()
        val opts = publishOptionsOf(props)

        opts.shouldNotBeNull()
        log.debug { "opts: $opts" }
    }

    @Test
    fun `publishOptionsOf with properties and builder applies expectedStream`() {
        val props = Properties()
        val opts = publishOptionsOf(props) {
            expectedStream("events")
        }

        log.debug { "opts: $opts" }
        opts.shouldNotBeNull()
        opts.expectedStream shouldBeEqualTo "events"
    }
}

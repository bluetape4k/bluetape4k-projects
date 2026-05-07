package io.bluetape4k.nats.client

import io.nats.client.Options
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

class OptionsTest {

    @Test
    fun `natsOptions with builder creates Options instance`() {
        val options = natsOptions {
            server(Options.DEFAULT_URL)
        }

        options.shouldNotBeNull()
        options.servers.first().toString() shouldBeEqualTo Options.DEFAULT_URL
    }

    @Test
    fun `natsOptions with properties creates Options from properties`() {
        val props = Properties().apply {
            setProperty(Options.PROP_URL, Options.DEFAULT_URL)
        }
        val options = natsOptions(props)

        options.shouldNotBeNull()
    }

    @Test
    fun `natsOptions with properties and builder allows override`() {
        val props = Properties()
        val options = natsOptions(props) {
            maxReconnects(10)
        }

        options.shouldNotBeNull()
        options.maxReconnect shouldBeEqualTo 10
    }

    @Test
    fun `natsOptionsOf uses default values`() {
        val options = natsOptionsOf()

        options.shouldNotBeNull()
        options.servers.first().toString() shouldBeEqualTo Options.DEFAULT_URL
        options.maxReconnect shouldBeEqualTo Options.DEFAULT_MAX_RECONNECT
        options.bufferSize shouldBeEqualTo Options.DEFAULT_BUFFER_SIZE
    }

    @Test
    fun `natsOptionsOf with custom parameters applies them`() {
        val options = natsOptionsOf(maxReconnects = 3, bufferSize = 32_768)

        options.shouldNotBeNull()
        options.maxReconnect shouldBeEqualTo 3
        options.bufferSize shouldBeEqualTo 32_768
    }
}

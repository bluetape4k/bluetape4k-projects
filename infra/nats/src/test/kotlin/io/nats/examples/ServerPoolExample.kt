package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.natsOptions
import io.bluetape4k.testcontainers.mq.NatsServer
import io.bluetape4k.utils.ShutdownQueue
import io.nats.client.Nats
import org.junit.jupiter.api.Test
import java.time.Duration

class ServerPoolExample: AbstractNatsTest() {

    companion object: KLogging() {
        private val natsServer1 by lazy { createNatsServer() }
        private val natsServer2 by lazy { createNatsServer() }
        private val natsServer3 by lazy { createNatsServer() }

        private fun createNatsServer(): NatsServer {
            return NatsServer(useDefaultPort = false, reuse = false).apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        private val BOOTSTRAPS by lazy {
            arrayOf(natsServer1.url, natsServer2.url, natsServer3.url)
        }
    }

    @Test
    fun `provide server list`() {
        val options = natsOptions {
            servers(BOOTSTRAPS)
            reconnectWait(Duration.ofSeconds(10))
            maxReconnects(10)
        }

        println("CONNECTING")
        Nats.connect(options).use { nc ->
            var si = nc.serverInfo
            println("CONNECTED 1")
            println("  to: ${si.host}:${si.port}")
            println("  discovered: ${si.connectURLs}")

            // WHILE THE THREAD IS SLEEPING, KILL THE SERVER WE ARE CONNECTED TO SO A RECONNECT OCCURS
            natsServer1.stop()
            Thread.sleep(1_000)

            si = nc.serverInfo
            println("CONNECT 2")
            println("  to: ${si.host}:${si.port}")
            println("  discovered: ${si.connectURLs}")
        }
    }
}

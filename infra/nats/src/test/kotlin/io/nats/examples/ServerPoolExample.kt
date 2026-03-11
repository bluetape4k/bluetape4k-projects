package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.natsOptions
import io.bluetape4k.testcontainers.mq.NatsServer
import io.nats.client.Nats
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerPoolExample: AbstractNatsTest() {

    companion object: KLogging()

    private lateinit var natsServer1: NatsServer
    private lateinit var natsServer2: NatsServer
    private lateinit var natsServer3: NatsServer

    private lateinit var bootstraps: Array<String>

    @BeforeAll
    fun startServers() {
        // 컨테이너를 순차적으로 시작해 Docker Desktop 동시 inspect 경쟁 조건을 방지합니다.
        natsServer1 = createNatsServer()
        natsServer2 = createNatsServer()
        natsServer3 = createNatsServer()
        bootstraps = arrayOf(natsServer1.url, natsServer2.url, natsServer3.url)
    }

    @AfterAll
    fun stopServers() {
        runCatching { natsServer3.stop() }
        runCatching { natsServer2.stop() }
        runCatching { natsServer1.stop() }
    }

    private fun createNatsServer(): NatsServer {
        return NatsServer(useDefaultPort = false, reuse = false).apply {
            withStartupAttempts(3)
            start()
        }
    }

    @Test
    fun `provide server list`() {
        val options = natsOptions {
            servers(bootstraps)
            reconnectWait(Duration.ofSeconds(10))
            maxReconnects(30)
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

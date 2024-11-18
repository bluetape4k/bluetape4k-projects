package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class ConsulServerTest {

    companion object: KLogging()

    @Test
    fun `launch Consul server`() {
        ConsulServer().use { consul ->
            consul.start()
            consul.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `launch Consul server with default port`() {
        ConsulServer(useDefaultPort = true).use { consul ->
            consul.start()
            consul.isRunning.shouldBeTrue()

            consul.port shouldBeEqualTo ConsulServer.HTTP_PORT
            consul.dnsPort shouldBeEqualTo ConsulServer.DNS_PORT
            consul.httpPort shouldBeEqualTo ConsulServer.HTTP_PORT
            consul.rpcPort shouldBeEqualTo ConsulServer.RPC_PORT
        }
    }
}

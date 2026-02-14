package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ConsulServerTest: AbstractContainerTest() {

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

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { ConsulServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { ConsulServer(tag = " ") }
    }
}

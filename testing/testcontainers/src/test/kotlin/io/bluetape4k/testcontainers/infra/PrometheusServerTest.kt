package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class PrometheusServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `launch prometheus server`() {
        PrometheusServer().use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `launch prometheus server with default port`() {
        PrometheusServer(useDefaultPort = true).use { server ->
            server.start()
            server.isRunning.shouldBeTrue()

            server.port shouldBeEqualTo PrometheusServer.PORT
            server.serverPort shouldBeEqualTo PrometheusServer.PORT
            server.pushgatewayPort shouldBeEqualTo PrometheusServer.PUSHGATEWAY_PORT
            server.graphiteExporterPort shouldBeEqualTo PrometheusServer.GRAPHITE_EXPORTER_PORT
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { PrometheusServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { PrometheusServer(tag = " ") }
    }
}

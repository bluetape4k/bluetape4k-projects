package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode


@Execution(ExecutionMode.SAME_THREAD)
class PrometheusServerTest {

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
}

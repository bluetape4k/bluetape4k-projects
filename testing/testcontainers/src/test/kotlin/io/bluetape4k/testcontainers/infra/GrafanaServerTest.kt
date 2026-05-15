package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.apache.hc.client5.http.fluent.Request
import org.junit.jupiter.api.Test

class GrafanaServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `launch grafana server`() {
        GrafanaServer().use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `launch grafana server with default port`() {
        GrafanaServer(useDefaultPort = true).use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
            server.port shouldBeEqualTo GrafanaServer.PORT
        }
    }

    @Test
    fun `GET api health returns 200`() {
        GrafanaServer().use { server ->
            server.start()
            val statusCode = Request.get("${server.url}/api/health")
                .execute()
                .returnResponse()
                .code
            statusCode shouldBeEqualTo 200
        }
    }

    @Test
    fun `provision prometheus datasource and verify via api`() {
        GrafanaServer().use { server ->
            server.start()
            server.withPrometheusDataSource("http://prometheus:9090")

            val body = Request.get("${server.url}/api/datasources")
                .addHeader("Authorization", basicAuth(GrafanaServer.ADMIN_USER, GrafanaServer.ADMIN_PASSWORD))
                .execute()
                .returnContent()
                .asString()

            body shouldContain "Prometheus"
        }
    }

    @Test
    fun `provision dashboard and verify via api`() {
        val minimalDashboard = """{"title":"Test Dashboard","panels":[],"schemaVersion":36}"""

        GrafanaServer().use { server ->
            server.start()
            server.withDashboard(minimalDashboard)

            val body = Request.get("${server.url}/api/search?type=dash-db")
                .addHeader("Authorization", basicAuth(GrafanaServer.ADMIN_USER, GrafanaServer.ADMIN_PASSWORD))
                .execute()
                .returnContent()
                .asString()

            body shouldContain "Test Dashboard"
        }
    }

    @Test
    fun `blank image or tag is not allowed`() {
        assertFailsWith<IllegalArgumentException> { GrafanaServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { GrafanaServer(tag = " ") }
    }

    private fun basicAuth(user: String, password: String): String {
        val encoded = java.util.Base64.getEncoder().encodeToString("$user:$password".toByteArray())
        return "Basic $encoded"
    }
}

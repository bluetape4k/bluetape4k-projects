package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.apache.hc.client5.http.fluent.Request
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Base64

@Tag("infra")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrometheusGrafanaIntegrationTest: AbstractContainerTest() {

    companion object: KLogging() {
        val prometheus: PrometheusServer by lazy { PrometheusServer.Launcher.prometheus }
        val grafana: GrafanaServer by lazy { GrafanaServer.Launcher.grafana }

        private val DASHBOARD_JSON =
            """{"title":"Prometheus Integration","panels":[],"schemaVersion":36}"""

        private fun basicAuth(): String {
            val encoded = Base64.getEncoder()
                .encodeToString("${GrafanaServer.ADMIN_USER}:${GrafanaServer.ADMIN_PASSWORD}".toByteArray())
            return "Basic $encoded"
        }

        private fun datasourceExists(): Boolean {
            val code = Request.get("${grafana.url}/api/datasources/name/Prometheus")
                .addHeader("Authorization", basicAuth())
                .execute()
                .returnResponse()
                .code
            return code == 200
        }
    }

    @BeforeAll
    fun setup() {
        prometheus.isRunning.shouldBeTrue()
        grafana.isRunning.shouldBeTrue()

        // Idempotent: skip POST if datasource already exists (container reuse)
        if (!datasourceExists()) {
            grafana.withPrometheusDataSource(prometheus.url)
        }

        // withDashboard uses overwrite:true — safe to call on every run
        grafana.withDashboard(DASHBOARD_JSON)
    }

    @Test
    fun `prometheus is reachable`() {
        val statusCode = Request.get("${prometheus.url}/-/ready")
            .execute()
            .returnResponse()
            .code
        statusCode shouldBeEqualTo 200
    }

    @Test
    fun `grafana datasource is connected to prometheus`() {
        val body = Request.get("${grafana.url}/api/datasources")
            .addHeader("Authorization", basicAuth())
            .execute()
            .returnContent()
            .asString()

        body shouldContain "Prometheus"
        body shouldContain prometheus.url
    }

    @Test
    fun `provisioned dashboard is retrievable via api`() {
        val body = Request.get("${grafana.url}/api/search?type=dash-db")
            .addHeader("Authorization", basicAuth())
            .execute()
            .returnContent()
            .asString()

        body shouldContain "Prometheus Integration"
    }
}

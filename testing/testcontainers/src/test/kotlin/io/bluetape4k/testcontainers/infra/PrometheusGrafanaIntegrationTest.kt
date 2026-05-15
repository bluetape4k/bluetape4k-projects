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

        // Stable UID ensures overwrite:true deduplicates correctly on container reuse
        private const val DASHBOARD_UID = "bluetape4k-prometheus-integration"
        private val DASHBOARD_JSON =
            """{"uid":"$DASHBOARD_UID","title":"Prometheus Integration","panels":[],"schemaVersion":36}"""

        private fun basicAuth(): String {
            val encoded = Base64.getEncoder()
                .encodeToString("${GrafanaServer.ADMIN_USER}:${GrafanaServer.ADMIN_PASSWORD}".toByteArray())
            return "Basic $encoded"
        }

        // Returns true only when a Prometheus datasource pointing at the current prometheus.url exists.
        // Handles container reuse: a stale datasource with a different URL is treated as absent.
        private fun datasourceUpToDate(): Boolean {
            val body = runCatching {
                Request.get("${grafana.url}/api/datasources/name/Prometheus")
                    .addHeader("Authorization", basicAuth())
                    .execute()
                    .returnContent()
                    .asString()
            }.getOrNull() ?: return false
            return body.contains(prometheus.url)
        }
    }

    @BeforeAll
    fun setup() {
        prometheus.isRunning.shouldBeTrue()
        grafana.isRunning.shouldBeTrue()

        // Idempotent: only provision when datasource is absent or points at a stale URL
        if (!datasourceUpToDate()) {
            grafana.withPrometheusDataSource(prometheus.url)
        }

        // Stable UID + overwrite:true → idempotent across container reuse
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
        val body = Request.get("${grafana.url}/api/dashboards/uid/$DASHBOARD_UID")
            .addHeader("Authorization", basicAuth())
            .execute()
            .returnContent()
            .asString()

        body shouldContain "Prometheus Integration"
    }
}

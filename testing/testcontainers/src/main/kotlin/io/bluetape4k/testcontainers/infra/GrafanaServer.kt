package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.util.Timeout
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Base64

/**
 * integration test를 위해 Docker에서 [Grafana](https://grafana.com/) server를 실행합니다.
 *
 * Follows the same `GenericServer + Launcher` pattern as [PrometheusServer].
 *
 * ## Usage
 *
 * ```kotlin
 * val grafana = GrafanaServer().apply { start() }
 * // grafana.url → "http://localhost:<mappedPort>"
 *
 * // Provision a Prometheus datasource after start:
 * grafana.withPrometheusDataSource("http://prometheus-host:9090")
 * ```
 *
 * ## Behavior / Contract
 *
 * - Wait strategy: `GET /api/health` returns HTTP 200 before the container is considered ready.
 * - Default credentials: admin / admin.
 * - [withPrometheusDataSource] and [withDashboard] must be called **after** [start].
 */
class GrafanaServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<GrafanaServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Grafana Docker Hub image name. */
        const val IMAGE = "grafana/grafana"

        /** Default Docker image tag. */
        const val TAG = "13.1.3"

        /** Server identifier used for system property registration. */
        const val NAME = "grafana"

        /** Grafana HTTP port. */
        const val PORT = 3000

        /** Default admin username. */
        const val ADMIN_USER = "admin"

        /** Default admin password. */
        const val ADMIN_PASSWORD = "admin"

        /**
         * 미리 구성된 [DockerImageName]으로 [GrafanaServer]를 생성합니다.
         *
         * @param imageName      Fully qualified Docker image name with tag.
         * @param useDefaultPort When `true`, binds port [PORT] to the same fixed host port.
         * @param reuse          Whether to reuse an existing container across test runs.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): GrafanaServer = GrafanaServer(imageName, useDefaultPort, reuse)

        /**
         * image name과 tag로 [GrafanaServer]를 생성합니다.
         *
         * @param image          Docker image name; blank value throws [IllegalArgumentException].
         * @param tag            Docker image tag; blank value throws [IllegalArgumentException].
         * @param useDefaultPort When `true`, binds port [PORT] to the same fixed host port.
         * @param reuse          Whether to reuse an existing container across test runs.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): GrafanaServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return GrafanaServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    init {
        addExposedPort(PORT)
        withReuse(reuse)
        withEnv("GF_SECURITY_ADMIN_USER", ADMIN_USER)
        withEnv("GF_SECURITY_ADMIN_PASSWORD", ADMIN_PASSWORD)

        setWaitStrategy(
            HttpWaitStrategy()
                .forPort(PORT)
                .forPath("/api/health")
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60))
        )

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * Provisions a Prometheus datasource in Grafana via the HTTP API.
     *
     * Must be called **after** [start].
     *
     * @param prometheusUrl Base URL of the Prometheus server (e.g. `"http://prometheus:9090"`).
     * @throws RuntimeException if the Grafana API returns a non-2xx status.
     */
    fun withPrometheusDataSource(prometheusUrl: String): GrafanaServer = apply {
        prometheusUrl.requireNotBlank("prometheusUrl")
        val escapedUrl = prometheusUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        val body = """{"name":"Prometheus","type":"prometheus","url":"$escapedUrl","access":"proxy","isDefault":true}"""
        grafanaPost("/api/datasources", body)
    }

    /**
     * Provisions a dashboard in Grafana from a JSON model string.
     *
     * Must be called **after** [start].
     *
     * @param dashboardJson Grafana dashboard JSON (the value of the `dashboard` property in the import format).
     * @throws RuntimeException if the Grafana API returns a non-2xx status.
     */
    fun withDashboard(dashboardJson: String): GrafanaServer = apply {
        dashboardJson.requireNotBlank("dashboardJson")
        val body = """{"dashboard":$dashboardJson,"overwrite":true,"folderId":0}"""
        grafanaPost("/api/dashboards/db", body)
    }

    private fun grafanaPost(path: String, jsonBody: String) {
        val credentials = Base64.getEncoder().encodeToString("$ADMIN_USER:$ADMIN_PASSWORD".toByteArray())
        val response = Request.post("$url$path")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Basic $credentials")
            .bodyString(jsonBody, ContentType.APPLICATION_JSON)
            .connectTimeout(Timeout.ofSeconds(10))
            .responseTimeout(Timeout.ofSeconds(10))
            .execute()
            .returnResponse()

        check(response.code in 200..299) {
            "Grafana API POST $path returned ${response.code}"
        }
    }

    /**
     * Singleton launcher for reuse across tests.
     */
    object Launcher {
        val grafana: GrafanaServer by lazy {
            GrafanaServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        val defaultGrafana: GrafanaServer by lazy {
            GrafanaServer(useDefaultPort = true).apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

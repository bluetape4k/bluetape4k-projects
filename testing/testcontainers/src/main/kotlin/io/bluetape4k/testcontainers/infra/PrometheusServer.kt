package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Docker를 이용하여 [Prometheus](https://prometheus.io/) 서버를 수행합니다.
 *
 * 참고: [Prometheus docker image](https://hub.docker.com/r/bitnami/prometheus)
 *
 * ```
 * val prometheusServer = PrometheusServer().apply {
 *     start()
 *     ShutdownQueue.register(this)
 * }
 * ```
 */
class PrometheusServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<PrometheusServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "prom/prometheus"
        const val TAG = "v3.7.3"
        const val NAME = "prometheus"

        const val PORT = 9090
        const val PUSHGATEWAY_PORT = 9091
        const val GRAPHITE_EXPORTER_PORT = 9109

        val EXPOSED_PORTS = intArrayOf(PORT, PUSHGATEWAY_PORT, GRAPHITE_EXPORTER_PORT)

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): PrometheusServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return PrometheusServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "https://$host:$port"

    val serverPort: Int get() = getMappedPort(PORT)
    val pushgatewayPort: Int get() = getMappedPort(PUSHGATEWAY_PORT)
    val graphiteExporterPort: Int get() = getMappedPort(GRAPHITE_EXPORTER_PORT)

    init {
        addExposedPorts(*EXPOSED_PORTS)
        withReuse(reuse)

        val waitStrategy = LogMessageWaitStrategy()
            .withRegEx(".*Server is ready to receive web requests.*")
            .withTimes(1)
            .withStartupTimeout(Duration.ofSeconds(5))

        setWaitStrategy(waitStrategy)

        if (useDefaultPort) {
            // 위에 addExposedPorts 를 등록했으면, 따로 지정하지 않으면 그 값들을 사용합니다.
            exposeCustomPorts(*EXPOSED_PORTS)
        }
    }

    override fun start() {
        super.start()

        val extraProps = mapOf<String, Any?>(
            "server.port" to serverPort,
            "pushgateway.port" to pushgatewayPort,
            "graphiteExporter.port" to graphiteExporterPort
        )
        writeToSystemProperties(NAME, extraProps)
    }

    object Launch {
        val prometheus: PrometheusServer by lazy {
            PrometheusServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }

    object Launcher {
        val prometheus: PrometheusServer by lazy {
            PrometheusServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        val defaultPrometheus: PrometheusServer by lazy {
            PrometheusServer(useDefaultPort = true).apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

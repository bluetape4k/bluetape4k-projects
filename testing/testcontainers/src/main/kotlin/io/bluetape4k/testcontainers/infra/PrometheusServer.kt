package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
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
): GenericContainer<PrometheusServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Prometheus 서버의 Docker Hub 이미지 이름입니다. */
        const val IMAGE = "prom/prometheus"

        /** 기본 Docker 이미지 태그입니다. */
        const val TAG = "v3.7.3"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 식별자입니다. */
        const val NAME = "prometheus"

        /** Prometheus HTTP API 및 Web UI 포트입니다. */
        const val PORT = 9090

        /** Prometheus Pushgateway 포트입니다. */
        const val PUSHGATEWAY_PORT = 9091

        /** Graphite Exporter 포트입니다. */
        const val GRAPHITE_EXPORTER_PORT = 9109

        /** 컨테이너에 노출할 포트 목록입니다. */
        val EXPOSED_PORTS = intArrayOf(PORT, PUSHGATEWAY_PORT, GRAPHITE_EXPORTER_PORT)

        /**
         * 이미지 이름/태그로 [PrometheusServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = PrometheusServer(image = "prom/prometheus", tag = "v3.7.3")
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 9090/9091/9109 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
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
    override val url: String get() = "http://$host:$port"

    /** Prometheus HTTP 포트의 매핑 결과입니다. */
    val serverPort: Int get() = getMappedPort(PORT)

    /** Pushgateway 포트의 매핑 결과입니다. */
    val pushgatewayPort: Int get() = getMappedPort(PUSHGATEWAY_PORT)

    /** Graphite exporter 포트의 매핑 결과입니다. */
    val graphiteExporterPort: Int get() = getMappedPort(GRAPHITE_EXPORTER_PORT)

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "host", "port", "url",
        "server-port", "pushgateway-port", "graphite-exporter-port",
    )

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "server-port" to serverPort.toString(),
        "pushgateway-port" to pushgatewayPort.toString(),
        "graphite-exporter-port" to graphiteExporterPort.toString(),
    )

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
        writeToSystemProperties()
    }

    /**
     * 하위 호환용 런처입니다. [Launcher]를 사용하세요.
     */
    @Deprecated("Use Launcher instead", ReplaceWith("Launcher"))
    object Launch {
        val prometheus: PrometheusServer by lazy {
            PrometheusServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }

    /**
     * 테스트에서 재사용할 Prometheus 서버 싱글턴을 제공합니다.
     */
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

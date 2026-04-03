package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.mq.PulsarServer.Launcher.PulsarClient
import io.bluetape4k.utils.ShutdownQueue
import org.apache.pulsar.client.api.ClientBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.testcontainers.pulsar.PulsarContainer
import org.testcontainers.utility.DockerImageName

/**
 * Apache Pulsar 테스트 서버 컨테이너를 실행하고 브로커 연결 정보를 제공합니다.
 *
 * ## 동작/계약
 * - 브로커 포트와 HTTP 포트를 함께 노출하고 `useDefaultPort=true`이면 고정 바인딩을 시도합니다.
 * - 인스턴스 생성만으로는 시작되지 않으며 `start()` 호출 시 broker 관련 시스템 프로퍼티를 기록합니다.
 * - `url`은 `pulsarBrokerUrl` 값을 그대로 반환합니다.
 *
 * ```kotlin
 * val server = PulsarServer()
 * server.start()
 * // server.url.startsWith("pulsar://") == true
 * ```
 *
 * 참고: [Apache Pulsar official images](https://hub.docker.com/r/apachepulsar/pulsar/tags)
 */
class PulsarServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): PulsarContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "apachepulsar/pulsar"
        const val TAG = "3.3.5"                    // NOTE: 4.+ 버전은 Java 17 이상 필요하다고 예외가 발생한다.
        const val NAME = "pulsar"
        const val PORT = BROKER_PORT
        const val HTTP_PORT = BROKER_HTTP_PORT

        /**
         * [DockerImageName]으로 [PulsarServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): PulsarServer {
            return PulsarServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [PulsarServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 수행해야 합니다.
         *
         * ```kotlin
         * val server = PulsarServer(image = "apachepulsar/pulsar", tag = "3.3.5")
         * // server.brokerPort > 0
         * ```
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): PulsarServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = pulsarBrokerUrl

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "host", "port", "url",
        "broker-url", "broker-port", "broker-http-port",
    )

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "broker-url" to pulsarBrokerUrl,
        "broker-port" to brokerPort.toString(),
        "broker-http-port" to brokerHttpPort.toString(),
    )

    /** Pulsar broker TCP 포트의 매핑 결과입니다. */
    val brokerPort: Int get() = getMappedPort(PORT)

    /** Pulsar broker HTTP 포트의 매핑 결과입니다. */
    val brokerHttpPort: Int get() = getMappedPort(HTTP_PORT)

    init {
        withReuse(reuse)
        addExposedPorts(PORT, HTTP_PORT)

        if (useDefaultPort) {
            exposeCustomPorts(PORT, HTTP_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트 전역에서 재사용할 Pulsar 서버와 클라이언트 생성 헬퍼를 제공합니다.
     *
     * ## 동작/계약
     * - `pulsar`는 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - `PulsarClient(...)`는 매 호출마다 새 클라이언트를 생성하며 호출자가 닫아야 합니다.
     */
    object Launcher {
        val pulsar by lazy {
            PulsarServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * broker URL 기준으로 [PulsarClient]를 생성합니다.
         *
         * ## 동작/계약
         * - `setup` 블록으로 [ClientBuilder]를 추가 설정한 뒤 `build()`합니다.
         * - 반환된 클라이언트는 자동 종료되지 않으므로 호출자가 `close()`해야 합니다.
         *
         * ```kotlin
         * val client = PulsarServer.Launcher.PulsarClient(PulsarServer.Launcher.pulsar.url)
         * // client != null
         * ```
         */
        inline fun PulsarClient(brokerUrl: String, setup: ClientBuilder.() -> Unit = {}): PulsarClient {
            return PulsarClient.builder()
                .serviceUrl(brokerUrl)
                .apply(setup)
                .build()
        }
    }
}

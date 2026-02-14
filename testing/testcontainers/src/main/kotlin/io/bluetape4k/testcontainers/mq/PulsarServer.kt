package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.apache.pulsar.client.api.ClientBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.testcontainers.containers.PulsarContainer
import org.testcontainers.utility.DockerImageName

/**
 * Docker를 이용하여 [Apache Pulsar](https://pulsar.apache.org/)를 구동해주는 container 입니다.
 *
 * 참고: [Apache Pulsar official images](https://hub.docker.com/r/apachepulsar/pulsar/tags)
 *
 * Exposed ports:
 * - 4222: NATS server
 * - 6222: NATS server for clustering
 * - 8222: NATS server for monitoring
 *
 * ```
 * // start nats server by docker
 * val nats = NatsServer().apply { start() }
 * ```
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class PulsarServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): PulsarContainer(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "apachepulsar/pulsar"
        const val TAG = "3.3.5"                    // NOTE: 4.+ 버전은 Java 17 이상 필요하다고 예외가 발생한다.
        const val NAME = "pulsar"
        const val PORT = BROKER_PORT
        const val HTTP_PORT = BROKER_HTTP_PORT

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): PulsarServer {
            return PulsarServer(imageName, useDefaultPort, reuse)
        }

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

    val brokerPort: Int get() = getMappedPort(PORT)
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

        val extraProps = mapOf(
            "broker.url" to pulsarBrokerUrl,
            "broker.port" to brokerPort,
            "broker.http.port" to brokerHttpPort
        )
        writeToSystemProperties(NAME, extraProps)
    }

    object Launcher {
        val pulsar by lazy {
            PulsarServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        inline fun PulsarClient(brokerUrl: String, setup: ClientBuilder.() -> Unit = {}): PulsarClient {
            return PulsarClient.builder()
                .serviceUrl(brokerUrl)
                .apply(setup)
                .build()
        }
    }
}

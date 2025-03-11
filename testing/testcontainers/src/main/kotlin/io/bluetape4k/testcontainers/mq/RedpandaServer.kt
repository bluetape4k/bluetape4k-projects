package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.redpanda.RedpandaContainer
import org.testcontainers.utility.DockerImageName


/**
 * Redpanda 를 Testcontainers 를 이용하여 실행시켜주는 클래스입니다.
 *
 * - 참고: [Redpanda Official Site](https://redpanda.com/)
 * - 참고: [Redpanda Docker Hub](https://hub.docker.com/r/redpandadata/redpanda)
 *
 * @param dockerImageName
 * @param useDefaultPort
 * @param reuse
 */
class RedpandaServer private constructor(
    dockerImageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): RedpandaContainer(dockerImageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "docker.redpanda.com/redpandadata/redpanda"
        const val TAG = "v24.3.1"
        const val NAME = "redpanda"

        const val PORT = 9092
        const val ADMIN_PORT = 9644
        const val SCHEMA_REGISTRY_PORT = 8081
        const val REST_PROXY_PORT = 8082

        @JvmStatic
        operator fun invoke(
            dockerImageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedpandaServer {
            return RedpandaServer(dockerImageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedpandaServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "redpanda://$host:$port"
    val adminPort: Int get() = getMappedPort(ADMIN_PORT)
    val schemaRegistryPort: Int get() = getMappedPort(SCHEMA_REGISTRY_PORT)
    val restProxyPort: Int get() = getMappedPort(REST_PROXY_PORT)

    init {
        addExposedPorts(PORT, ADMIN_PORT, SCHEMA_REGISTRY_PORT, REST_PROXY_PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT, ADMIN_PORT, SCHEMA_REGISTRY_PORT, REST_PROXY_PORT)
        }
    }

    override fun start() {
        super.start()

        val extraProps = mapOf(
            "admin.port" to adminPort,
            "schema.registry.port" to schemaRegistryPort,
            "rest.proxy.port" to restProxyPort,
        )
        writeToSystemProperties(NAME, extraProps)
    }

    object Launcher {
        val redpanda: RedpandaServer by lazy {
            RedpandaServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import io.nats.client.Connection
import io.nats.client.Nats
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * Docker를 이용하여 [nats](http://nats.io)를 구동해주는 container 입니다.
 *
 * 참고: [Nats official images](https://hub.docker.com/_/nats?tab=description&page=1&ordering=last_updated)
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
class NatsServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<NatsServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "nats"
        const val TAG = "2.10"
        const val NAME = "nats"

        const val NATS_PORT = 4222
        const val NATS_CLUSTER_PORT = 6222
        const val NATS_MONITOR_PORT = 8222

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): NatsServer {
            return NatsServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): NatsServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(NATS_PORT)
    override val url: String get() = "$NAME://$host:$port"

    val natsPort: Int get() = getMappedPort(NATS_PORT)
    val clusterPort: Int get() = getMappedPort(NATS_CLUSTER_PORT)
    val monitorPort: Int get() = getMappedPort(NATS_MONITOR_PORT)

    init {
        addExposedPorts(NATS_PORT, NATS_CLUSTER_PORT, NATS_MONITOR_PORT)
        withReuse(reuse)

        // JetStream 을 사용하기 위해서 지정
        // 참고; [Nats Commandline Options](https://hub.docker.com/_/nats)
        withCommand("-js")

        if (useDefaultPort) {
            // 위에 addExposedPorts 를 등록했으면, 따로 지정하지 않으면 그 값들을 사용합니다.
            exposeCustomPorts(NATS_PORT, NATS_CLUSTER_PORT, NATS_MONITOR_PORT)
        }
    }

    override fun start() {
        super.start()

        val extraProps = mapOf(
            "cluster.port" to clusterPort,
            "monitor.port" to monitorPort,
        )
        writeToSystemProperties(NAME, extraProps)
    }

    object Launcher {
        val nats: NatsServer by lazy {
            NatsServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

inline fun <T> withNats(url: String, block: Connection.() -> T): T {
    return Nats.connect(url).use { connection: Connection ->
        block(connection)
    }
}

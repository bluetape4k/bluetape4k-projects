package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainers로 single-node [etcd](https://etcd.io/) server를 실행합니다.
 *
 * ## Behavior / Contract
 * - Exposes the etcd client API on [CLIENT_PORT] and the peer API on [PEER_PORT].
 * - [endpoint] is suitable for jetcd `Client.builder().endpoints(...)`.
 * - The container is reusable by default and can be shared through [Launcher.etcd].
 * - Starting the server exports connection properties under `testcontainers.etcd.*`.
 *
 * ```kotlin
 * val etcd = EtcdServer.Launcher.etcd
 * val endpoint = etcd.endpoint
 * ```
 */
class EtcdServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<EtcdServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Official etcd container registry recommended by the etcd container guide. */
        const val IMAGE = "gcr.io/etcd-development/etcd"

        /** Default etcd image tag. */
        const val TAG = "v3.6.0"

        /** System property namespace. */
        const val NAME = "etcd"

        /** etcd client API port. */
        const val CLIENT_PORT = 2379

        /** etcd peer communication port. */
        const val PEER_PORT = 2380

        /** Ports exported by the container. */
        val EXPORT_PORTS = intArrayOf(CLIENT_PORT, PEER_PORT)

        /**
         * [DockerImageName]으로 [EtcdServer]를 생성합니다.
         *
         * @param imageName Docker image name.
         * @param useDefaultPort binds 2379/2380 to the same host ports when `true`.
         * @param reuse enables reusable Testcontainers when `true`.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): EtcdServer {
            return EtcdServer(imageName, useDefaultPort, reuse)
        }

        /**
         * image name과 tag로 [EtcdServer]를 생성합니다.
         *
         * @param image Docker image name; blank values are rejected.
         * @param tag Docker image tag; blank values are rejected.
         * @param useDefaultPort binds 2379/2380 to the same host ports when `true`.
         * @param reuse enables reusable Testcontainers when `true`.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): EtcdServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = clientPort
    override val url: String get() = endpoint

    /** Mapped etcd client API port. */
    val clientPort: Int get() = getMappedPort(CLIENT_PORT)

    /** Mapped etcd peer port. */
    val peerPort: Int get() = getMappedPort(PEER_PORT)

    /** HTTP endpoint for etcd v3 clients such as jetcd. */
    val endpoint: String get() = "http://$host:$clientPort"

    /** Single endpoint list for APIs that accept multiple etcd endpoints. */
    val endpoints: List<String> get() = listOf(endpoint)

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "client-port", "peer-port", "endpoint")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "client-port" to clientPort.toString(),
        "peer-port" to peerPort.toString(),
        "endpoint" to endpoint,
    )

    init {
        addExposedPorts(*EXPORT_PORTS)
        withReuse(reuse)
        waitingFor(Wait.forHttp("/health").forPort(CLIENT_PORT).forStatusCode(200))
        withCommand(
            "/usr/local/bin/etcd",
            "--name",
            NAME,
            "--data-dir",
            "/etcd-data",
            "--listen-client-urls",
            "http://0.0.0.0:$CLIENT_PORT",
            "--advertise-client-urls",
            "http://127.0.0.1:$CLIENT_PORT",
            "--listen-peer-urls",
            "http://0.0.0.0:$PEER_PORT",
            "--initial-advertise-peer-urls",
            "http://127.0.0.1:$PEER_PORT",
            "--initial-cluster",
            "$NAME=http://127.0.0.1:$PEER_PORT",
        )

        if (useDefaultPort) {
            exposeCustomPorts(*EXPORT_PORTS)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * Shared singleton launcher for tests that need a reusable etcd server.
     */
    object Launcher {
        val etcd: EtcdServer by lazy {
            EtcdServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

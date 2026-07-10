package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName

/**
 * Runs a [K3s](https://k3s.io/) lightweight Kubernetes cluster in Docker for integration tests.
 *
 * Extends the native testcontainers [K3sContainer] and adds the bluetape4k
 * `GenericServer + Launcher` pattern.
 *
 * ## Usage
 *
 * ```kotlin
 * val k3s = K3sServer().apply { start() }
 * val client = k3s.kubernetesClient()
 * client.namespaces().list().items  // at least 1 (default)
 * ```
 *
 * ## Behavior / Contract
 *
 * - Requires a Docker daemon that supports `--privileged` mode (handled by [K3sContainer]).
 * - Wait strategy: K3s API server ready (handled by [K3sContainer]).
 * - [kubernetesClient] must be called **after** [start].
 * - CI note: K3s requires a privileged Docker runner. Tag tests with `@Tag("k8s")` for nightly-only.
 */
class K3sServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): K3sContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** K3s Docker Hub image name. */
        const val IMAGE = "rancher/k3s"

        /** Default Docker image tag. */
        const val TAG = "v1.32.4-k3s1"

        /** Server identifier used for system property registration. */
        const val NAME = "k3s"

        /** Kubernetes API server port. */
        const val API_PORT = 6443

        /**
         * Creates a [K3sServer] from a pre-built [DockerImageName].
         *
         * @param imageName      Fully qualified Docker image name with tag.
         * @param useDefaultPort When `true`, binds port [API_PORT] to the same fixed host port.
         * @param reuse          Whether to reuse an existing container across test runs.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): K3sServer = K3sServer(imageName, useDefaultPort, reuse)

        /**
         * Creates a [K3sServer] by image name and tag.
         *
         * @param image          Docker image name; blank value throws [IllegalArgumentException].
         * @param tag            Docker image tag; blank value throws [IllegalArgumentException].
         * @param useDefaultPort When `true`, binds port [API_PORT] to the same fixed host port.
         * @param reuse          Whether to reuse an existing container across test runs.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): K3sServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return K3sServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(API_PORT)
    override val url: String get() = "https://$host:$port"

    /** Kubeconfig YAML with the server URL patched to the mapped host and port. */
    val kubeConfig: String get() = kubeConfigYaml

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    init {
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(API_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * Returns a fabric8 [KubernetesClient] pre-configured for this K3s cluster.
     *
     * Must be called **after** [start]. Each call returns a **new** client instance
     * registered in [ShutdownQueue] — it will be closed on JVM shutdown automatically.
     * Use `.use { }` to close earlier when the client is no longer needed.
     *
     * **Runtime dependency required**: consumers must add `io.fabric8:kubernetes-client`
     * to their runtime classpath; this library declares it as `compileOnly`.
     */
    fun kubernetesClient(): KubernetesClient {
        val config = Config.fromKubeconfig(kubeConfig)
        return KubernetesClientBuilder().withConfig(config).build()
            .also { ShutdownQueue.register(it) }
    }

    /**
     * Singleton launcher for reuse across tests.
     *
     * Note: K3s containers are slow to start. Prefer the singleton over creating new instances per test class.
     */
    object Launcher {
        val k3s: K3sServer by lazy {
            K3sServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

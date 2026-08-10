package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.net.URI

/**
 * Testcontainers wrapper for the [Floci](https://github.com/floci-io/floci) AWS emulator.
 *
 * Floci is a lightweight AWS emulator built as a GraalVM Native Image. It is
 * the default open-source replacement for LocalStack Community edition after
 * the LocalStack Community repository was archived on 2026-03-23.
 *
 * This class wraps the official Docker image directly with [GenericContainer].
 * Use the pinned [TAG] for reproducible tests. Floci also publishes `-compat`
 * image tags that include AWS CLI and boto3 for init scripts, but the standard
 * image is sufficient for AWS SDK/Testcontainers integration tests.
 *
 * Floci enables all AWS services by default. Calling [withServices] is a no-op
 * kept for [AwsEmulatorServer] and [LocalStackServer] migration compatibility.
 *
 * ```kotlin
 * val server = FlociServer()
 * server.start()
 *
 * val endpoint: URI = server.awsEndpoint
 * val accessKey: String = server.awsAccessKey
 * val secretKey: String = server.awsSecretKey
 * val region: String = server.regionName
 * ```
 *
 * References: [Floci GitHub](https://github.com/floci-io/floci) ·
 * [Docker image](https://hub.docker.com/r/floci/floci/tags)
 *
 * @param imageName Docker image name.
 * @param useDefaultPort Whether to bind the default port 4566 directly.
 * @param reuse Whether to enable Testcontainers reuse.
 */
class FlociServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<FlociServer>(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {

    companion object: KLogging() {
        /** Floci Docker image repository. */
        const val IMAGE = "floci/floci"

        /** Default pinned Floci Docker image tag. */
        const val TAG = "1.6.0"

        /** PropertyExportingServer namespace and container identifier. */
        const val NAME = "floci"

        /** Single AWS emulator port exposed by Floci. */
        const val PORT = 4566

        /** Default access key. Floci accepts any non-empty credential value. */
        const val DEFAULT_ACCESS_KEY = "test"

        /** Default secret key. Floci accepts any non-empty credential value. */
        const val DEFAULT_SECRET_KEY = "test"

        /** Default AWS region. */
        const val DEFAULT_REGION = "us-east-1"

        /**
         * Creates a [FlociServer] from an image repository and tag.
         *
         * ```kotlin
         * val server = FlociServer(image = "floci/floci", tag = FlociServer.TAG)
         * server.start()
         * // server.url.startsWith("http://") == true
         * ```
         *
         * @param image Docker image repository. Blank values are rejected.
         * @param tag Docker image tag. Blank values are rejected.
         * @param useDefaultPort Whether to bind the default port 4566 directly.
         * @param reuse Whether to enable Testcontainers reuse.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): FlociServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * Creates a [FlociServer] from a [DockerImageName].
         *
         * ```kotlin
         * val image = DockerImageName.parse("floci/floci").withTag(FlociServer.TAG)
         * val server = FlociServer(image)
         * // server.isRunning == false before start()
         * ```
         *
         * @param imageName Docker image name.
         * @param useDefaultPort Whether to bind the default port 4566 directly.
         * @param reuse Whether to enable Testcontainers reuse.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): FlociServer {
            return FlociServer(imageName, useDefaultPort, reuse)
        }
    }

    /** Mapped Floci port for the running container. */
    override val port: Int get() = getMappedPort(PORT)

    /** Base URL of the running Floci server, for example `http://localhost:32768`. */
    override val url: String get() = "http://$host:$port"

    /**
     * AWS emulator endpoint URI.
     *
     * Use this value as the AWS SDK `endpointOverride`.
     */
    override val awsEndpoint: URI get() = URI.create("http://$host:$port")

    /** Default AWS access key. Floci accepts any non-empty credential value. */
    override val awsAccessKey: String = DEFAULT_ACCESS_KEY

    /** Default AWS secret key. Floci accepts any non-empty credential value. */
    override val awsSecretKey: String = DEFAULT_SECRET_KEY

    /** Default AWS region name. */
    override val regionName: String = DEFAULT_REGION

    /** PropertyExportingServer namespace. */
    override val propertyNamespace: String = NAME

    /**
     * Returns the keys exported to system properties.
     *
     * All keys are lower-case kebab-case values.
     */
    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "aws-endpoint", "aws-access-key", "aws-secret-key", "region")

    /**
     * Returns the key/value map exported to system properties.
     *
     * Call this only after the container has started so [host] and [port] are valid.
     */
    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "aws-endpoint" to awsEndpoint.toString(),
        "aws-access-key" to awsAccessKey,
        "aws-secret-key" to awsSecretKey,
        "region" to regionName,
    )

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        // Floci does not expose a dedicated health endpoint, so wait for the listening port.
        setWaitStrategy(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * Accepts the AWS service list for migration compatibility.
     *
     * Floci always enables all services, so this method is a no-op and returns
     * the same [FlociServer] instance.
     *
     * @param services Ignored service names.
     * @return This [FlociServer] instance for method chaining.
     */
    override fun withServices(vararg services: String): FlociServer {
        log.debug { "Floci enables all services by default. withServices(${services.toList()}) is a no-op." }
        return this
    }

    /**
     * Starts the container and exports [properties] to system properties.
     */
    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * Provides a shared [FlociServer] instance for tests in one JVM.
     *
     * The singleton starts on first access and is registered in [ShutdownQueue]
     * for JVM shutdown cleanup. It never requests Docker-level container reuse,
     * so separate test JVMs receive isolated Floci containers.
     */
    object Launcher {
        /**
         * Lazily initialized [FlociServer] singleton.
         *
         * ```kotlin
         * val floci = FlociServer.Launcher.floci
         * val endpoint = floci.awsEndpoint
         * ```
         */
        val floci: FlociServer by lazy {
            FlociServer(reuse = false).apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

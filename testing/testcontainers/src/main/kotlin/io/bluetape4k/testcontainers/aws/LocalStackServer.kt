package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI

/**
 * Testcontainers wrapper for LocalStack.
 *
 * LocalStack Community edition was archived on 2026-03-23. New open-source AWS
 * emulator tests should use [FlociServer]. Keep this wrapper only for migration
 * compatibility or for paid LocalStack Pro deployments that still require the
 * LocalStack image.
 *
 * ```kotlin
 * val server = LocalStackServer()
 *    .withNetwork(network)
 *    .withNetworkAliases("notthis", "localstack")
 * server.start()
 * ```
 *
 * [LocalStack Docker image](https://hub.docker.com/r/localstack/localstack/tags)
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "LocalStack Community edition was archived on 2026-03-23. Use FlociServer for open-source AWS emulator tests.",
    replaceWith = ReplaceWith("FlociServer", "io.bluetape4k.testcontainers.aws.FlociServer"),
    level = DeprecationLevel.WARNING
)
class LocalStackServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): LocalStackContainer(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {

    companion object: KLogging() {
        const val IMAGE = "localstack/localstack"
        const val NAME = "localstack"
        const val TAG = "4"
        const val PORT = 4566

        /**
         * Creates a [LocalStackServer].
         *
         * @param image Docker image repository.
         * @param tag Docker image tag.
         * @param useDefaultPort Whether to bind the default port 4566 directly.
         * @param reuse Whether to enable Testcontainers reuse.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): LocalStackServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * Creates a [LocalStackServer] from a [DockerImageName].
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
        ): LocalStackServer {
            return LocalStackServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    // Prefix AwsEmulatorServer properties with `aws` to avoid JVM signature
    // collisions with LocalStackContainer Java getters.
    override val awsEndpoint: URI get() = this.getEndpoint()
    override val awsAccessKey: String get() = this.getAccessKey()
    override val awsSecretKey: String get() = this.getSecretKey()
    override val regionName: String get() = this.getRegion()

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "aws-endpoint", "aws-access-key", "aws-secret-key", "region")

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

        setWaitStrategy(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun withServices(vararg services: String): LocalStackServer {
        super.withServices(*services.map { it.lowercase() }.toTypedArray())
        return this
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * Provides reusable [LocalStackServer] instances for tests.
     */
    object Launcher {

        val services = listOf(
            "cloudwatch",
            "logs",
            "dynamodb",
            "kinesis",
            "kms",
            "s3",
            "ses",
            "sns",
            "sqs",
            "sts"
        )

        /**
         * Creates and starts the default [LocalStackServer] instance.
         */
        val localStack: LocalStackServer by lazy {
            getLocalStack(*services.toTypedArray())
        }

        /**
         * Creates and starts a [LocalStackServer] with the requested services.
         */
        fun getLocalStack(vararg services: String): LocalStackServer {
            return LocalStackServer().apply {
                withServices(*services)
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

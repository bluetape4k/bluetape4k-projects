package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
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
 * Runs Amazon DynamoDB Local with Testcontainers.
 *
 * ## Behavior / Contract
 * - Exposes DynamoDB Local on [PORT] and provides [awsEndpoint] for AWS SDK clients.
 * - Uses in-memory, shared-database mode by default.
 * - The container is reusable by default and can be shared through [Launcher.dynamoDb].
 * - Starting the server exports connection properties under `testcontainers.dynamodb-local.*`.
 *
 * ```kotlin
 * val dynamoDb = DynamoDbLocalServer.Launcher.dynamoDb
 * val endpoint = dynamoDb.awsEndpoint
 * ```
 */
class DynamoDbLocalServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<DynamoDbLocalServer>(imageName), GenericServer, PropertyExportingServer, AwsEmulatorServer {

    companion object: KLogging() {
        /** Official Amazon DynamoDB Local Docker image. */
        const val IMAGE = "amazon/dynamodb-local"

        /** Default DynamoDB Local image tag. */
        const val TAG = "2.6.1"

        /** Property namespace for exported connection properties. */
        const val NAME = "dynamodb-local"

        /** DynamoDB Local service port. */
        const val PORT = 8000

        /** Default access key accepted by local AWS emulators. */
        const val DEFAULT_ACCESS_KEY = "test"

        /** Default secret key accepted by local AWS emulators. */
        const val DEFAULT_SECRET_KEY = "test"

        /** Default region used by tests. */
        const val DEFAULT_REGION = "us-east-1"

        /**
         * Creates a [DynamoDbLocalServer] from an image name and tag.
         *
         * @param image Docker image name; blank values are rejected.
         * @param tag Docker image tag; blank values are rejected.
         * @param useDefaultPort binds [PORT] to the same host port when `true`.
         * @param reuse enables reusable Testcontainers when `true`.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): DynamoDbLocalServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            return invoke(DockerImageName.parse(image).withTag(tag), useDefaultPort, reuse)
        }

        /**
         * Creates a [DynamoDbLocalServer] from a [DockerImageName].
         *
         * @param imageName Docker image name.
         * @param useDefaultPort binds [PORT] to the same host port when `true`.
         * @param reuse enables reusable Testcontainers when `true`.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = false,
        ): DynamoDbLocalServer {
            return DynamoDbLocalServer(imageName, useDefaultPort, reuse)
        }
    }

    /** Mapped DynamoDB Local port. */
    override val port: Int get() = getMappedPort(PORT)

    /** DynamoDB Local base URL. */
    override val url: String get() = "http://$host:$port"

    /** AWS SDK endpoint override URI. */
    override val awsEndpoint: URI get() = URI.create(url)

    /** Alias for [awsEndpoint] for DynamoDB-focused tests. */
    val endpoint: URI get() = awsEndpoint

    /** Default AWS access key. */
    override val awsAccessKey: String = DEFAULT_ACCESS_KEY

    /** Default AWS secret key. */
    override val awsSecretKey: String = DEFAULT_SECRET_KEY

    /** Default AWS region name. */
    override val regionName: String = DEFAULT_REGION

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "endpoint", "aws-endpoint", "aws-access-key", "aws-secret-key", "region")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "endpoint" to endpoint.toString(),
        "aws-endpoint" to awsEndpoint.toString(),
        "aws-access-key" to awsAccessKey,
        "aws-secret-key" to awsSecretKey,
        "region" to regionName,
    )

    init {
        addExposedPorts(PORT)
        withReuse(reuse)
        withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-sharedDb")
        waitingFor(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * DynamoDB Local always starts the DynamoDB service, so service selection is a no-op.
     */
    override fun withServices(vararg services: String): DynamoDbLocalServer {
        if (services.isNotEmpty()) {
            log.warn { "withServices(${services.toList()}) is a no-op: DynamoDB Local exposes only DynamoDB." }
        }
        return this
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * Shared singleton launcher for tests that need a reusable DynamoDB Local server.
     */
    object Launcher {
        val dynamoDb: DynamoDbLocalServer by lazy {
            DynamoDbLocalServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

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
 * LocalStackServer : 'a fully funcational local AWS cloud stack'
 *
 * ```
 * // Run S3 Server
 * val s3Server = LocalStackServer().start()
 * ```
 *
 * ```
 * val server = LocalStackServer()
 *    .withNetwork(network)
 *    .withNetworkAliases("notthis", "localstack")
 *    .start()
 * ```
 *
 * [Locakstack docker image](https://hub.docker.com/r/localstack/localstack/tags)
 */
@Deprecated(
    message = "LocalStack Community edition은 2026-03-23에 archived 되었습니다. FlociServer 또는 유료 LocalStack Pro 사용을 검토하세요.",
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
         * [LocalStackServer]를 생성합니다.
         *
         * @param image docker image (기본: `localstack/localstack`)
         * @param tag docker image tag (기본: `3.6`)
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): LocalStackServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [LocalStackServer]를 생성합니다.
         *
         * @param imageName docker image name
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): LocalStackServer {
            return LocalStackServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    // AwsEmulatorServer 프로퍼티 구현
    // 프로퍼티 이름에 `aws` 접두어를 붙여 LocalStackContainer 의 Java getter 와의 JVM 시그니처 충돌을 방지한다.
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
     * [LocalStackServer]용 Launcher
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
         * [LocalStackServer] 인스턴스를 생성하고 시작합니다.
         */
        val localStack: LocalStackServer by lazy {
            getLocalStack(*services.toTypedArray())
        }

        /**
         * [LocalStackServer] 인스턴스를 생성하고 시작합니다.
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

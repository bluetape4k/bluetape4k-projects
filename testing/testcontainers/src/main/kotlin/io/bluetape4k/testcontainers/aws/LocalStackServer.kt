package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

/**
 * LocalStackServer : 'a fully funcational local AWS cloud stack'
 *
 * ```
 * // Run S3 Server
 * val s3Server = LocalStackServer().withServices(Service.S3).start()
 * ```
 *
 * ```
 * val server = LocalStackServer()
 *    .withNetwork(network)
 *    .withNetworkAliases("notthis", "localstack")
 *    .withServices(Service.S3, Service.SQS, Service.CLOUDWATCHLOGS)
 *    .start()
 * ```
 *
 * [Locakstack docker image](https://hub.docker.com/r/localstack/localstack/tags)
 */
class LocalStackServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): LocalStackContainer(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "localstack/localstack"
        const val NAME = "localstack"
        const val TAG = "3.6"
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
            return LocalStackServer(imageName, useDefaultPort, reuse)
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

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        setWaitStrategy(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    override fun withServices(vararg services: Service): LocalStackServer = apply {
        super.withServices(*services)
    }

    fun getCredentialProvider(): StaticCredentialsProvider {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(this.accessKey, this.secretKey))
    }

    /**
     * [LocalStackServer]용 Launcher
     */
    object Launcher {
        /**
         * [LocalStackServer] 인스턴스를 생성하고 시작합니다.
         */
        val localStack: LocalStackServer by lazy {
            LocalStackServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

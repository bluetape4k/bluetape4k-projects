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
 * [Zipkin](https://zipkin.io/) 서버의 docker image를 testcontainers 환경 하에서 실행하는 클래스입니다.
 *
 * 참고: [Zipkin Docker image](https://hub.docker.com/r/openzipkin/zipkin/tags)
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class ZipkinServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<ZipkinServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "openzipkin/zipkin-slim"
        const val NAME = "zipkin"
        const val TAG = "2.23"      // NOTE: 2.24 이상에서는 예외가 발생합니다. 꼭 2.23으로 고정하세요!!!
        const val PORT = 9411

        /**
         * [DockerImageName]으로 [ZipkinServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("openzipkin/zipkin-slim").withTag("2.23")
         * val server = ZipkinServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 9411 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZipkinServer {
            return ZipkinServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [ZipkinServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = ZipkinServer(image = "openzipkin/zipkin-slim", tag = "2.23")
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 9411 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZipkinServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

//        withEnv("JAVA_OPTS", "--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions")

        setWaitStrategy(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Zipkin 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val zipkin: ZipkinServer by lazy {
            ZipkinServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        val defaultZipkin: ZipkinServer by lazy {
            ZipkinServer(useDefaultPort = true).apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
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
): GenericContainer<ZipkinServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "openzipkin/zipkin-slim"
        const val NAME = "zipkin"
        const val TAG = "2.23"      // NOTE: 2.24 이상에서는 예외가 발생합니다. 꼭 2.23으로 고정하세요!!!
        const val PORT = 9411

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZipkinServer {
            return ZipkinServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ZipkinServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            
            val imageName = DockerImageName.parse(IMAGE).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "https://$host:$port"

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
        writeToSystemProperties(NAME)
    }

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

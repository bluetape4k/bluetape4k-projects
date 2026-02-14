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
 * [Jaeger](https://www.jaegertracing.io/) Tracing 서버의 docker image를 testcontainers 환경 하에서 실행하는 클래스입니다.
 *
 * 참고: [Jaeger Docker image](https://hub.docker.com/r/jaegertracing/all-in-one/tags)
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class JaegerServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<JaegerServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "jaegertracing/all-in-one"
        const val NAME = "jaeger"
        const val TAG = "1"

        const val ZIPKIN_PORT = 9411
        const val FRONTEND_PORT = 16686
        const val CONFIG_PORT = 5778
        const val THRIFT_PORT = 14268

        val EXPOSED_PORT = intArrayOf(ZIPKIN_PORT, FRONTEND_PORT, CONFIG_PORT, THRIFT_PORT)

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): JaegerServer {
            return JaegerServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): JaegerServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            
            val imageName = DockerImageName.parse(IMAGE).withTag(tag)
            return JaegerServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(FRONTEND_PORT)
    override val url: String get() = "https://$host:$port"

    val frontendPort: Int get() = getMappedPort(FRONTEND_PORT)
    val zipkinPort: Int get() = getMappedPort(ZIPKIN_PORT)
    val configPort: Int get() = getMappedPort(CONFIG_PORT)
    val thriftPort: Int get() = getMappedPort(THRIFT_PORT)

    init {
        addExposedPorts(*EXPOSED_PORT)
        withReuse(reuse)

        val wait = Wait.forLogMessage(".*Query server started.*\\s", 1)
        setWaitStrategy(wait)

        if (useDefaultPort) {
            exposeCustomPorts(*EXPOSED_PORT)
        }
    }

    override fun start() {
        super.start()

        val extraProps = mapOf<String, Any?>(
            "frontend.port" to frontendPort,
            "zipkin.port" to zipkinPort,
            "config.port" to configPort,
            "thrift.port" to thriftPort,
        )
        writeToSystemProperties(NAME, extraProps)
    }

    object Launcher {
        val jaeger: JaegerServer by lazy {
            JaegerServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

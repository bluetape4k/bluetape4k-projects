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
): GenericContainer<JaegerServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "jaegertracing/all-in-one"
        const val NAME = "jaeger"
        const val TAG = "1"

        const val ZIPKIN_PORT = 9411
        const val FRONTEND_PORT = 16686
        const val CONFIG_PORT = 5778
        const val THRIFT_PORT = 14268

        val EXPOSED_PORT = intArrayOf(ZIPKIN_PORT, FRONTEND_PORT, CONFIG_PORT, THRIFT_PORT)

        /**
         * [DockerImageName]으로 [JaegerServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("jaegertracing/all-in-one").withTag("1")
         * val server = JaegerServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 9411/16686/5778/14268 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): JaegerServer {
            return JaegerServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [JaegerServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = JaegerServer(image = "jaegertracing/all-in-one", tag = "1")
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): JaegerServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return JaegerServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(FRONTEND_PORT)
    override val url: String get() = "http://$host:$port"

    /** Jaeger UI(Frontend) 포트의 매핑 결과입니다. */
    val frontendPort: Int get() = getMappedPort(FRONTEND_PORT)

    /** Zipkin 호환 수집 포트의 매핑 결과입니다. */
    val zipkinPort: Int get() = getMappedPort(ZIPKIN_PORT)

    /** 설정 조회용 포트의 매핑 결과입니다. */
    val configPort: Int get() = getMappedPort(CONFIG_PORT)

    /** Thrift 수집 포트의 매핑 결과입니다. */
    val thriftPort: Int get() = getMappedPort(THRIFT_PORT)

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "host", "port", "url",
        "frontend-port", "zipkin-port", "config-port", "thrift-port",
    )

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "frontend-port" to frontendPort.toString(),
        "zipkin-port" to zipkinPort.toString(),
        "config-port" to configPort.toString(),
        "thrift-port" to thriftPort.toString(),
    )

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
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Jaeger 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val jaeger: JaegerServer by lazy {
            JaegerServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

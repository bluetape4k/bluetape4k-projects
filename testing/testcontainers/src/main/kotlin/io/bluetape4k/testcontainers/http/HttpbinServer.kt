package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * `httpbin` API를 로컬 Docker 컨테이너로 실행하는 테스트 서버입니다.
 *
 * ## 동작/계약
 * - 컨테이너 생성 시점에는 시작하지 않으며, `start()` 호출 후에만 요청을 받을 수 있습니다.
 * - `useDefaultPort=true`이면 `80` 포트를 호스트에 고정 바인딩하려고 시도하고, 아니면 동적 포트를 사용합니다.
 * - `reuse=true`일 때 Testcontainers 재사용 옵션을 켭니다.
 *
 * ```kotlin
 * val server = HttpbinServer(useDefaultPort = false)
 * server.start()
 * val endpoint = "${server.url}/get"
 * // endpoint.endsWith("/get") == true
 * ```
 *
 * 불안정한 [httpbin.org](https://httpbin.org/) 대신 로컬 테스트에서 사용할 수 있습니다.
 * 실제 외부 네트워크 테스트는 [nghttp2.org](https://nghttp2.org/httpbin) 사용을 권장합니다.
 */
class HttpbinServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<HttpbinServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "kong/httpbin"
        const val TAG = "latest"
        const val NAME = "httpbin"
        const val PORT = 80

        /**
         * [DockerImageName]으로 [HttpbinServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 서버 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않으며 호출자가 `start()`를 직접 호출해야 합니다.
         * - `useDefaultPort`에 따라 `80` 포트 고정 바인딩 여부가 초기화 시점에 결정됩니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("kong/httpbin").withTag("latest")
         * val server = HttpbinServer(image, useDefaultPort = true)
         * // server.exposedPorts.contains(80) == true
         * ```
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinServer {
            return HttpbinServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름과 태그로 [HttpbinServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 `requireNotBlank` 검증으로 [IllegalArgumentException]이 발생합니다.
         * - `DockerImageName.parse(image).withTag(tag)`로 이미지를 구성한 뒤 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val server = HttpbinServer(image = "kong/httpbin", tag = "latest")
         * // server.url.startsWith("http://") == true
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 80 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinServer {
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
        withExposedPorts(PORT)
        withReuse(reuse)

        // 대기전략을 쓰니, port 설정이 안되었다는 예외가 발생한다.
        // waitingFor(Wait.forHttp("/"))

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트 전역에서 재사용할 [HttpbinServer] 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - `httpbin`에 처음 접근할 때 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 이후 접근에서는 이미 시작된 동일 인스턴스를 반환합니다.
     *
     * ```kotlin
     * val server = HttpbinServer.Launcher.httpbin
     * // server.isRunning == true
     * ```
     */
    object Launcher {
        /** 지연 초기화되는 재사용용 Httpbin 서버입니다. */
        val httpbin: HttpbinServer by lazy {
            HttpbinServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

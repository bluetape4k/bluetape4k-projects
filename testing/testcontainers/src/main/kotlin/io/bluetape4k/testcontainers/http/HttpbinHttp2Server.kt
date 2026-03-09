package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * HTTP/2(h2c) 지원 `httpbin` API를 로컬 Docker 컨테이너로 실행하는 테스트 서버입니다.
 *
 * ## 동작/계약
 * - `/get` 엔드포인트에 대한 HTTP 헬스체크가 `200`을 반환할 때까지(최대 60초) 시작을 기다립니다.
 * - 포트 오픈만 확인하지 않고 실제 요청 성공을 기준으로 준비 상태를 판정합니다.
 * - `useDefaultPort=true`이면 `8000` 포트를 호스트에 고정 바인딩하려고 시도하고, 아니면 동적 포트를 사용합니다.
 * - 인스턴스 생성만으로는 컨테이너가 시작되지 않으며, `start()` 호출이 필요합니다.
 *
 * ```kotlin
 * val server = HttpbinHttp2Server()
 * server.start()
 * val endpoint = "${server.url}/get"
 * // endpoint == "http://${server.host}:${server.port}/get"
 * ```
 *
 * Docker 이미지: `skydoctor/httpbin-http2`
 */
class HttpbinHttp2Server private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<HttpbinHttp2Server>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "skydoctor/httpbin-http2"
        const val TAG = "latest"
        const val NAME = "httpbin-http2"
        const val PORT = 8000

        /**
         * [DockerImageName]으로 [HttpbinHttp2Server] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 서버 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("skydoctor/httpbin-http2").withTag("latest")
         * val server = HttpbinHttp2Server(image)
         * // server.isRunning == false
         * ```
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinHttp2Server {
            return HttpbinHttp2Server(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름과 태그로 [HttpbinHttp2Server] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환한 뒤 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 수행해야 합니다.
         *
         * ```kotlin
         * val server = HttpbinHttp2Server(image = "skydoctor/httpbin-http2", tag = "latest")
         * // server.url.startsWith("http://") == true
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 8000 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinHttp2Server {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int
        get() = getMappedPort(PORT)

    override val url: String
        get() = "http://$host:$port"

    init {
        withExposedPorts(PORT)
        withReuse(reuse)
        waitingFor(
            // HINT: h2c 환경에서 HTTP/1.x 헬스체크가 502를 반환하는 경우를 피하기 위한 선택
            Wait.forHttp("/get")
                .forPort(PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60))
        )

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    /**
     * 테스트 전역에서 재사용할 [HttpbinHttp2Server] 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 이후 접근에서는 동일 인스턴스를 반환합니다.
     *
     * ```kotlin
     * val server = HttpbinHttp2Server.Launcher.httpbinHttp2
     * // server.isRunning == true
     * ```
     */
    object Launcher {
        /** 지연 초기화되는 HTTP/2 httpbin 서버입니다. */
        val httpbinHttp2: HttpbinHttp2Server by lazy {
            HttpbinHttp2Server().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

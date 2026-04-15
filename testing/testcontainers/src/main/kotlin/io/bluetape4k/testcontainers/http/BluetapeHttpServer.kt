package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * `bluetape4k/mock-server` Docker 이미지를 로컬 컨테이너로 실행하는 테스트 HTTP 서버입니다.
 *
 * ## 동작/계약
 * - `/ping` 엔드포인트가 HTTP 200을 반환할 때까지(최대 60초) 시작을 기다립니다.
 * - `useDefaultPort=true`이면 [PORT](8888) 포트를 호스트에 고정 바인딩하려고 시도하고,
 *   그렇지 않으면 동적 포트를 사용합니다.
 * - `reuse=true`일 때 Testcontainers 재사용 옵션을 켭니다.
 * - 인스턴스 생성만으로는 컨테이너가 시작되지 않으며, `start()` 호출이 필요합니다.
 *
 * ```kotlin
 * val server = BluetapeHttpServer()
 * server.start()
 * val pingUrl = "${server.url}/ping"
 * // pingUrl == "http://${server.host}:${server.port}/ping"
 * ```
 *
 * Docker 이미지: `bluetape4k/mock-server`
 */
class BluetapeHttpServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<BluetapeHttpServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Docker 이미지 이름 */
        const val IMAGE = "bluetape4k/mock-server"

        /** Docker 이미지 태그 */
        const val TAG = "latest"

        /** 서버 이름 (시스템 프로퍼티 네임스페이스로 사용됨) */
        const val NAME = "bluetape-http"

        /** 컨테이너 내부 포트 */
        const val PORT = 8888

        /**
         * [DockerImageName]으로 [BluetapeHttpServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 서버 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않으며 호출자가 `start()`를 직접 호출해야 합니다.
         * - `useDefaultPort`에 따라 포트 고정 바인딩 여부가 초기화 시점에 결정됩니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("bluetape4k/mock-server").withTag("latest")
         * val server = BluetapeHttpServer(image, useDefaultPort = false)
         * // server.isRunning == false
         * ```
         *
         * @param imageName Docker 이미지 이름
         * @param useDefaultPort `true`면 8888 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): BluetapeHttpServer {
            return BluetapeHttpServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름과 태그로 [BluetapeHttpServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 `requireNotBlank` 검증으로 [IllegalArgumentException]이 발생합니다.
         * - `DockerImageName.parse(image).withTag(tag)`로 이미지를 구성한 뒤 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val server = BluetapeHttpServer(image = "bluetape4k/mock-server", tag = "latest")
         * // server.url.startsWith("http://") == true
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 8888 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): BluetapeHttpServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    /** 호스트에 매핑된 실제 포트 번호 */
    override val port: Int get() = getMappedPort(PORT)

    /** 서버 기본 URL (`http://host:port`) */
    override val url: String get() = "http://$host:$port"

    /** httpbin 시뮬레이터 기본 URL (`http://host:port/httpbin`) */
    val httpbinUrl: String get() = "$url/httpbin"

    /** jsonplaceholder 시뮬레이터 기본 URL (`http://host:port/jsonplaceholder`) */
    val jsonplaceholderUrl: String get() = "$url/jsonplaceholder"

    /** 목업 웹 컨텐츠 기본 URL (`http://host:port/web`) */
    val webUrl: String get() = "$url/web"

    /** 시스템 프로퍼티 내보내기에 사용할 네임스페이스 */
    override val propertyNamespace: String = NAME

    /**
     * 내보낼 시스템 프로퍼티 키 목록을 반환합니다.
     *
     * ```kotlin
     * val keys = server.propertyKeys()
     * // keys == setOf("host", "port", "url", "httpbinUrl", "jsonplaceholderUrl", "webUrl")
     * ```
     */
    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "httpbinUrl", "jsonplaceholderUrl", "webUrl")

    /**
     * 현재 서버 접속 정보를 프로퍼티 맵으로 반환합니다.
     *
     * ```kotlin
     * val props = server.properties()
     * // props["httpbinUrl"] == "http://localhost:8888/httpbin"
     * ```
     */
    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "httpbinUrl" to httpbinUrl,
        "jsonplaceholderUrl" to jsonplaceholderUrl,
        "webUrl" to webUrl,
    )

    init {
        withExposedPorts(PORT)
        withReuse(reuse)
        waitingFor(
            Wait.forHttp("/ping")
                .forPort(PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60))
        )

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * 컨테이너를 시작하고 접속 정보를 시스템 프로퍼티에 내보냅니다.
     *
     * ## 동작/계약
     * - `super.start()`로 컨테이너를 시작한 뒤, [writeToSystemProperties]를 호출해
     *   `bluetape4k-mock-server.host`, `bluetape4k-mock-server.port`, `bluetape4k-mock-server.url`
     *   프로퍼티를 시스템에 등록합니다.
     *
     * ```kotlin
     * server.start()
     * // System.getProperty("bluetape4k-mock-server.url") != null
     * ```
     */
    override fun start() {
        super.start()
        log.info { "BluetapeHttpServer started. url=$url" }
        writeToSystemProperties()
    }

    /**
     * 테스트 전역에서 재사용할 [BluetapeHttpServer] 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - [bluetapeHttpServer]에 처음 접근할 때 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 이후 접근에서는 이미 시작된 동일 인스턴스를 반환합니다.
     *
     * ```kotlin
     * val server = BluetapeHttpServer.Launcher.bluetapeHttpServer
     * // server.isRunning == true
     * ```
     */
    object Launcher {
        /** 지연 초기화되는 재사용용 Bluetape HTTP 서버입니다. */
        val bluetapeHttpServer: BluetapeHttpServer by lazy {
            BluetapeHttpServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

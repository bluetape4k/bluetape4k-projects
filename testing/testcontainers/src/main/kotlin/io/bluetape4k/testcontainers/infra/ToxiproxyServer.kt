package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.toxiproxy.ToxiproxyContainer
import org.testcontainers.utility.DockerImageName

/**
 * [Toxiproxy](https://github.com/Shopify/toxiproxy)를 Testcontainers를 이용하여 실행하는 카오스 테스트 서버입니다.
 *
 * Toxiproxy는 네트워크 지연, 연결 차단, 대역폭 제한 등 다양한 네트워크 장애를 시뮬레이션할 수 있는
 * 프록시 서버입니다. 카오스 엔지니어링 및 장애 내성(resilience) 테스트에 활용됩니다.
 *
 * ## 지원 이미지
 * - `ghcr.io/shopify/toxiproxy`
 * - `shopify/toxiproxy`
 *
 * ## 노출 포트
 * - 컨트롤 API: `8474`
 * - 프록시 포트: `8666 ~ 8697` (최대 32개)
 *
 * ## 사용 예시
 * ```kotlin
 * val network = Network.newNetwork()
 * val upstream = HttpbinServer().apply { withNetwork(network); withNetworkAliases("httpbin"); start() }
 * val toxiproxy = ToxiproxyServer().apply { withNetwork(network); start() }
 *
 * val client = ToxiproxyClient(toxiproxy.host, toxiproxy.port)
 * val proxy = client.createProxy("httpbin", "0.0.0.0:8666", "httpbin:80")
 * // proxy를 통해 httpbin에 접근하여 장애 주입 테스트 수행
 * ```
 *
 * @param imageName Docker 이미지 이름
 * @param useDefaultPort `true`이면 컨트롤 포트와 프록시 포트를 호스트의 동일 번호로 고정 바인딩합니다.
 * @param reuse `true`이면 Testcontainers 재사용 옵션을 활성화합니다.
 */
class ToxiproxyServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): ToxiproxyContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Toxiproxy 컨테이너 기본 이미지 이름입니다. */
        const val IMAGE = "ghcr.io/shopify/toxiproxy"

        /** Toxiproxy 컨테이너 기본 태그입니다. */
        const val TAG = "2.9.0"

        /** 서버 이름 식별자입니다 (System Property 키 접두사로 사용됩니다). */
        const val NAME = "toxiproxy"

        /** Toxiproxy HTTP 컨트롤 API 포트입니다. */
        const val CONTROL_PORT = 8474

        /**
         * [DockerImageName]으로 [ToxiproxyServer] 인스턴스를 생성합니다.
         *
         * @param imageName Docker 이미지 이름
         * @param useDefaultPort `true`이면 컨트롤 포트와 프록시 포트를 호스트의 동일 번호로 고정 바인딩합니다.
         * @param reuse Testcontainers 재사용 여부
         * @return 새로운 [ToxiproxyServer] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ToxiproxyServer {
            return ToxiproxyServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름과 태그 문자열로 [ToxiproxyServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image` 또는 `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 컨테이너를 시작하지 않으며 호출자가 `start()`를 직접 호출해야 합니다.
         *
         * ```kotlin
         * val server = ToxiproxyServer(image = "ghcr.io/shopify/toxiproxy", tag = "2.9.0")
         * server.start()
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`이면 컨트롤 포트와 프록시 포트를 호스트의 동일 번호로 고정 바인딩합니다.
         * @param reuse Testcontainers 재사용 여부
         * @return 새로운 [ToxiproxyServer] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ToxiproxyServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return ToxiproxyServer(imageName, useDefaultPort, reuse)
        }
    }

    /**
     * 호스트에서 접근 가능한 Toxiproxy 컨트롤 API의 매핑된 포트입니다.
     */
    override val port: Int get() = getMappedPort(CONTROL_PORT)

    /**
     * Toxiproxy 컨트롤 API의 URL입니다.
     *
     * 예: `http://localhost:55123`
     */
    override val url: String get() = "http://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "control-port", "control-url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "control-port" to port.toString(),
        "control-url" to url,
    )

    init {
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts()
        }
    }

    /**
     * 컨테이너 시작 후 컨트롤 포트와 URL을 System Property로 등록합니다.
     *
     * 등록되는 속성:
     * - `testcontainers.toxiproxy.host`
     * - `testcontainers.toxiproxy.port`
     * - `testcontainers.toxiproxy.url`
     * - `testcontainers.toxiproxy.control-port`
     * - `testcontainers.toxiproxy.control-url`
     */
    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트 전역에서 재사용할 [ToxiproxyServer] 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - `toxiproxy`에 처음 접근할 때 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 이후 접근에서는 이미 시작된 동일 인스턴스를 반환합니다.
     * - 싱글턴이므로 toxic 상태가 테스트 간에 누수될 수 있습니다.
     *   상태 격리가 필요한 경우 각 테스트에서 직접 인스턴스를 생성하세요.
     *
     * ```kotlin
     * val server = ToxiproxyServer.Launcher.toxiproxy
     * // server.isRunning == true
     * ```
     */
    object Launcher {
        /** 지연 초기화되는 재사용용 Toxiproxy 서버입니다. */
        val toxiproxy: ToxiproxyServer by lazy {
            ToxiproxyServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

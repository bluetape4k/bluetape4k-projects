package io.bluetape4k.testcontainers.http

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * [WireMock](https://wiremock.org/)을 Docker 컨테이너로 실행하는 테스트 서버입니다.
 *
 * HTTP/HTTPS 요청을 스텁(stub)하거나 목(mock) 처리할 수 있는 서버를 제공하며,
 * 통합 테스트에서 외부 HTTP 의존성을 대체하는 데 유용합니다.
 *
 * ```kotlin
 * val wireMock = WireMockServer()
 * wireMock.start()
 *
 * wireMock.stubFor(
 *     get("/hello").willReturn(ok("Hello, World!"))
 * )
 *
 * // GET http://wireMock.baseUrl/hello → 200 "Hello, World!"
 * ```
 *
 * 참고: [WireMock Docker Hub](https://hub.docker.com/r/wiremock/wiremock)
 *
 * @param imageName         Docker 이미지 이름
 * @param useDefaultPort    기본 HTTP 포트 사용 여부
 * @param reuse             컨테이너 재사용 여부
 */
class WireMockServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<WireMockServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "wiremock/wiremock"
        const val TAG = "3.13.2"
        const val NAME = "wiremock"
        const val HTTP_PORT = 8080
        const val HTTPS_PORT = 8443

        /**
         * [WireMockServer]를 생성합니다.
         *
         * @param image         Docker 이미지 이름 (기본: `wiremock/wiremock`)
         * @param tag           Docker 이미지 태그 (기본: `3.13.2`)
         * @param useDefaultPort 기본 HTTP 포트를 사용할지 여부 (기본: `false`)
         * @param reuse         컨테이너 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): WireMockServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [WireMockServer]를 생성합니다.
         *
         * @param imageName     Docker 이미지 이름
         * @param useDefaultPort 기본 HTTP 포트를 사용할지 여부 (기본: `false`)
         * @param reuse         컨테이너 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): WireMockServer {
            return WireMockServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(HTTP_PORT)
    override val url: String get() = "http://$host:$port"

    /**
     * HTTPS 엔드포인트 URL을 반환합니다.
     */
    val httpsUrl: String get() = "https://$host:${getMappedPort(HTTPS_PORT)}"

    /**
     * WireMock 서버의 베이스 URL (HTTP)을 반환합니다.
     */
    val baseUrl: String get() = url

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "http.port", "https.port", "base.url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "http.port" to port.toString(),
        "https.port" to getMappedPort(HTTPS_PORT).toString(),
        "base.url" to baseUrl,
    )

    private var wireMockClient: WireMock? = null

    init {
        withExposedPorts(HTTP_PORT, HTTPS_PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(HTTP_PORT)
        }
    }

    override fun start() {
        super.start()
        wireMockClient = WireMock(host, port)
        writeToSystemProperties()
    }

    /**
     * WireMock stub을 등록합니다.
     */
    fun stubFor(mappingBuilder: MappingBuilder): StubMapping =
        wireMockClient!!.register(mappingBuilder)

    /**
     * 등록된 모든 stub과 요청 기록을 초기화합니다.
     */
    fun resetAll() {
        wireMockClient!!.resetMappings()
        wireMockClient!!.resetRequests()
    }

    /**
     * 테스트에서 재사용할 WireMock 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val wireMock: WireMockServer by lazy {
            WireMockServer().apply {
                start()
                ShutdownQueue.register(this as AutoCloseable)
            }
        }
    }
}

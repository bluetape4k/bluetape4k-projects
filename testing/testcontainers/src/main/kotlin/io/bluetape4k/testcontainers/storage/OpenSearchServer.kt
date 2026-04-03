package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.opensearch.testcontainers.OpenSearchContainer
import org.testcontainers.utility.DockerImageName

/**
 * OpenSearch 서버를 Docker 컨테이너로 실행하는 테스트용 래퍼입니다.
 *
 * ## 동작/계약
 * - HTTP(9200), TCP(9300) 포트를 노출하며 `useDefaultPort=true`이면 호스트 고정 바인딩을 시도합니다.
 * - 인스턴스 생성만으로는 시작되지 않고, `start()` 호출 후 시스템 프로퍼티를 기록합니다.
 * - `url`은 OpenSearch HTTP 엔드포인트(`httpHostAddress`)를 반환합니다.
 *
 * ```kotlin
 * val server = OpenSearchServer()
 * server.start()
 * // server.url.startsWith("http://") == true
 * ```
 *
 * Link: [Opensearch Docker images](https://hub.docker.com/r/opensearchproject/opensearch/tags)
 */
class OpenSearchServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): OpenSearchContainer<OpenSearchServer>(imageName), GenericServer, PropertyExportingServer {

    companion object Companion: KLogging() {
        const val IMAGE = "opensearchproject/opensearch"
        const val TAG = "3"
        const val NAME = "opensearch"

        const val HTTP_PORT = 9200
        const val TCP_PORT = 9300

        /**
         * [DockerImageName]으로 [OpenSearchServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 수행하지 않으며 `start()`는 호출자가 수행해야 합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("opensearchproject/opensearch").withTag("3")
         * val server = OpenSearchServer(image)
         * // server.isRunning == false
         * ```
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): OpenSearchServer {
            return OpenSearchServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [OpenSearchServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val server = OpenSearchServer(image = "opensearchproject/opensearch", tag = "3")
         * // server.url.contains(\":\") == true
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 9200/9300 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): OpenSearchServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(HTTP_PORT)
    override val url: String get() = httpHostAddress

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    init {
        addExposedPorts(HTTP_PORT, TCP_PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(HTTP_PORT, TCP_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트 전역에서 재사용할 OpenSearch 서버와 클라이언트 설정 헬퍼를 제공합니다.
     *
     * ## 동작/계약
     * - `openSearch`는 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - `getClientConfiguration`은 매 호출마다 새 [ClientConfiguration]을 반환하며 서버 상태는 변경하지 않습니다.
     *
     * ```kotlin
     * val server = OpenSearchServer.Launcher.openSearch
     * val config = OpenSearchServer.Launcher.getClientConfiguration(server)
     * // config != null
     * ```
     */
    object Launcher {
        /**
         * 기본 [OpenSearchServer]를 제공합니다.
         */
        val openSearch: OpenSearchServer by lazy {
            OpenSearchServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

    }
}

package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

/**
 * Elasticsearch OSS 서버를 Docker 컨테이너로 실행하는 테스트용 래퍼입니다.
 *
 * ## 동작/계약
 * - HTTP(9200), TCP(9300) 포트를 노출하며 `useDefaultPort=true`이면 호스트 고정 바인딩을 시도합니다.
 * - 인스턴스 생성만으로는 시작되지 않으며, `start()` 호출 시 시스템 프로퍼티를 기록합니다.
 * - `url`은 `httpHostAddress`를 반환합니다.
 *
 * ```kotlin
 * val server = ElasticsearchOssServer()
 * server.start()
 * // server.url.startsWith("http://") == true
 * ```
 *
 * Link: [Elasticsearch OSS Docker Images](https://www.docker.elastic.co/r/elasticsearch/elasticsearch-oss)
 */
class ElasticsearchOssServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): ElasticsearchContainer(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "docker.elastic.co/elasticsearch/elasticsearch-oss"
        const val TAG = "7.10.2"

        const val NAME = "elasticsearch-oss"
        const val DEFAULT_PASSWORD = "changeme"
        const val PORT = 9200
        const val TCP_PORT = 9300

        /**
         * 이미지 이름/태그로 [ElasticsearchOssServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val server = ElasticsearchOssServer(image = ElasticsearchOssServer.IMAGE, tag = "7.10.2")
         * // server.isRunning == false
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
        ): ElasticsearchOssServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [ElasticsearchOssServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`를 직접 호출해야 합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse(ElasticsearchOssServer.IMAGE).withTag("7.10.2")
         * val server = ElasticsearchOssServer(image)
         * // server.port > 0
         * ```
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ElasticsearchOssServer {
            return ElasticsearchOssServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = httpHostAddress

    init {
        addExposedPorts(PORT, TCP_PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT, TCP_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    /**
     * 테스트 전역에서 재사용할 Elasticsearch OSS 서버와 클라이언트 설정 헬퍼를 제공합니다.
     *
     * ## 동작/계약
     * - `elasticsearchOssServer`는 첫 접근 시 시작되고 [ShutdownQueue]에 종료 훅이 등록됩니다.
     * - `getClientConfiguration`은 Basic Auth(`elastic/changeme`)가 포함된 새 [ClientConfiguration]을 반환합니다.
     *
     * ```kotlin
     * val server = ElasticsearchOssServer.Launcher.elasticsearchOssServer
     * val config = ElasticsearchOssServer.Launcher.getClientConfiguration(server)
     * // config != null
     * ```
     */
    object Launcher {
        /** 지연 초기화되는 재사용용 Elasticsearch OSS 서버입니다. */
        val elasticsearchOssServer: ElasticsearchOssServer by lazy {
            ElasticsearchOssServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * Spring Data Elasticsearch용 [ClientConfiguration]을 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 서버의 `url`을 대상 엔드포인트로 사용합니다.
         * - 인증 정보는 `elastic / changeme` 고정값을 사용합니다.
         * - 새 설정 객체를 반환하며 서버 상태는 변경하지 않습니다.
         *
         * ```kotlin
         * val config = ElasticsearchOssServer.Launcher.getClientConfiguration(server)
         * // config != null
         * ```
         */
        fun getClientConfiguration(elasticsearch: ElasticsearchOssServer): ClientConfiguration {
            return ClientConfiguration.builder()
                .connectedTo(elasticsearch.url)
                .withBasicAuth("elastic", ElasticsearchOssServer.DEFAULT_PASSWORD)
                .build()
        }
    }
}

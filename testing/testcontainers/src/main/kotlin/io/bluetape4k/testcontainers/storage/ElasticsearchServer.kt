package io.bluetape4k.testcontainers.storage

import io.bluetape4k.LibraryName
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName


/**
 * Elasticsearch Server 를 Docker container 로 실행해주는 클래스입니다.
 *
 * Link: [Elasticsearch Docker images](https://www.docker.elastic.co/r/elasticsearch)
 *
 * ```
 * val elasticsearchServer = ElasticsearchServer().apply { start() }
 * ```
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class ElasticsearchServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
    /** `elastic` 사용자 기본 비밀번호입니다. */
    val password: String,
): ElasticsearchContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "docker.elastic.co/elasticsearch/elasticsearch"
        const val TAG = "9.3.3"

        const val NAME = "elasticsearch"
        const val DEFAULT_PASSWORD = LibraryName

        const val PORT = 9200
        const val TCP_PORT = 9300

        /**
         * [DockerImageName]으로 [ElasticsearchServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse(ElasticsearchServer.IMAGE).withTag(ElasticsearchServer.TAG)
         * val server = ElasticsearchServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 9200/9300 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         * @param password       `elastic` 사용자 비밀번호
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            password: String = DEFAULT_PASSWORD,
        ): ElasticsearchServer {
            return ElasticsearchServer(imageName, useDefaultPort, reuse, password)
        }

        /**
         * 이미지 이름/태그로 [ElasticsearchServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = ElasticsearchServer(image = ElasticsearchServer.IMAGE, tag = ElasticsearchServer.TAG)
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 9200/9300 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         * @param password       `elastic` 사용자 비밀번호
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            password: String = DEFAULT_PASSWORD,
        ): ElasticsearchServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse, password)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = httpHostAddress

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

    init {
        addExposedPorts(PORT, TCP_PORT)
        withReuse(reuse)
        withPassword(password)

        if (useDefaultPort) {
            exposeCustomPorts(PORT, TCP_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Elasticsearch 서버 싱글턴과 클라이언트 설정 헬퍼를 제공합니다.
     */
    object Launcher {
        /**
         * 기본 [ElasticsearchServer]를 제공합니다.
         */
        val elasticsearch: ElasticsearchServer by lazy {
            ElasticsearchServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

    }
}

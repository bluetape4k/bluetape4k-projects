package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

/**
 * [Elasticsearch](https://www.elastic.co/kr/elasticsearch) Open Source 용 Server 를 Docker container 로 실행해주는 클래스입니다.
 *
 * Link: [Elasticsearch OSS Docker Images](https://www.docker.elastic.co/r/elasticsearch/elasticsearch-oss)
 *
 * ```
 * val elasticsearchOssServer = ElasticsearchOssServer().apply { start() }
 * ```
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
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

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ElasticsearchOssServer {
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

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

    object Launcher {
        val elasticsearchOssServer: ElasticsearchOssServer by lazy {
            ElasticsearchOssServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * Spring Data Elasticsearch 를 사용 할 때 사용할 클라이언트 설정을 제공합니다.
         *
         * @param elasticsearch [ElasticsearchServer] 인스턴스
         * @return Spring Data Elasticsearch에서 제공하는 [ClientConfiguration] 인스턴스
         */
        fun getClientConfiguration(elasticsearch: ElasticsearchOssServer): ClientConfiguration {
            return ClientConfiguration.builder()
                .connectedTo(elasticsearch.url)
                .withBasicAuth("elastic", ElasticsearchOssServer.DEFAULT_PASSWORD)
                .build()
        }
    }
}

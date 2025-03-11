package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.opensearch.testcontainers.OpensearchContainer
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.testcontainers.utility.DockerImageName

/**
 * [Opensearch](https://opensearch.org/) Server 를 Docker container 로 실행해주는 클래스입니다.
 *
 * Link: [Opensearch Docker images](https://hub.docker.com/r/opensearchproject/opensearch/tags)
 *
 * ```
 * val elasticsearchServer = ElasticsearchServer().apply { start() }
 * ```
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class OpensearchServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): OpensearchContainer<OpensearchServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "opensearchproject/opensearch"
        const val TAG = "1"
        const val NAME = "opensearch"

        const val HTTP_PORT = 9200
        const val TCP_PORT = 9300

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): OpensearchServer {
            return OpensearchServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): OpensearchServer {
            val imageName = DockerImageName.parse("$image:$tag")
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(HTTP_PORT)
    override val url: String get() = httpHostAddress

    init {
        addExposedPorts(HTTP_PORT, TCP_PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(HTTP_PORT, TCP_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    object Launcher {
        /**
         * 기본 [OpensearchServer]를 제공합니다.
         */
        val opensearch: OpensearchServer by lazy {
            OpensearchServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * Spring Data Elasticsearch 를 사용 할 때 사용할 클라이언트 설정을 제공합니다.
         *
         * @param opensearch [OpensearchServer] 인스턴스
         * @return Spring Data Elasticsearch에서 제공하는 [ClientConfiguration] 인스턴스
         */
        fun getClientConfiguration(opensearch: OpensearchServer): ClientConfiguration {
            return ClientConfiguration.builder()
                .connectedTo(opensearch.url)
                //.usingSsl(opensearch.createSslContextFromCa())
                .withBasicAuth(opensearch.username, opensearch.password)
                .build()
        }
    }
}

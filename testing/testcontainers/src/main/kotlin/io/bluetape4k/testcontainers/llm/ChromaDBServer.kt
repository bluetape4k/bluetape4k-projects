package io.bluetape4k.testcontainers.llm

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.chromadb.ChromaDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * ChromaDB 테스트 컨테이너입니다.
 *
 * - 참고: [ChromaDB](https://www.trychroma.com/)
 * - 참고: [ChromaDB Docker Image](https://hub.docker.com/r/chromadb/chroma)
 */
class ChromaDBServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): ChromaDBContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** ChromaDB 서버의 Docker Hub 이미지 이름입니다. */
        const val IMAGE = "chromadb/chroma"

        /** 기본 Docker 이미지 태그입니다. */
        const val TAG = "0.5.23" // "latest"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 식별자입니다. */
        const val NAME = "chromadb"

        /** ChromaDB HTTP API 포트입니다. */
        const val PORT = 8000

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ChromaDBServer {
            return ChromaDBServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ChromaDBServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = firstMappedPort
    override val url: String get() = endpoint

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

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 ChromaDB 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val chromaDBServer: ChromaDBServer by lazy {
            ChromaDBServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }

}

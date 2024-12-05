package io.bluetape4k.testcontainers.llm

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.ollama.OllamaContainer
import org.testcontainers.utility.DockerImageName

/**
 * Local LLM 을 사용할 수 있는 [Ollama](https://ollama.com/) 를 Docker 환경에서 제공해주는 서버입니다.
 *
 * - 참고: [Ollama API](https://github.com/ollama/ollama/blob/main/docs/api.md) 를 참고하여 LLM Chat 을 수행할 수 있습니다.
 * - 참고: [Ollama Docker image](https://hub.docker.com/r/ollama/ollama) 를 참고하여 Docker image 를 사용할 수 있습니다.
 */
class OllamaServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): OllamaContainer(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "ollama/ollama"
        const val TAG = "0.4.7"
        const val NAME = "ollama"
        const val PORT = 11434

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): OllamaServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): OllamaServer {
            return OllamaServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    init {
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    object Launcher {
        val ollama: OllamaServer by lazy {
            OllamaServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

package io.bluetape4k.testcontainers.llm

import com.github.dockerjava.api.model.DeviceRequest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

/**
 * Local LLM 을 사용할 수 있는 [Ollama](https://ollama.com/) 를 Docker 환경에서 제공해주는 서버입니다.
 *
 * NOTE: OllamaContainer 가 getPort() 함수를 만들어서, GenericServer 인터페이스와 충돌이 생깁니다.
 * NOTE: 그래서 OllamaContainer를 사용하지 않고, 직접 구현했습니다.
 *
 * - 참고: [Ollama API](https://github.com/ollama/ollama/blob/main/docs/api.md) 를 참고하여 LLM Chat 을 수행할 수 있습니다.
 * - 참고: [Ollama Docker image](https://hub.docker.com/r/ollama/ollama) 를 참고하여 Docker image 를 사용할 수 있습니다.
 */
class OllamaServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<OllamaServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "ollama/ollama"
        const val TAG = "0.5.11"
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
            imageName.assertCompatibleWith(DockerImageName.parse(IMAGE))
            return OllamaServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    init {

        val info = this.dockerClient.infoCmd().exec()
        info.runtimes?.let { runtimes ->
            if (runtimes.containsKey("nvidia")) {
                withCreateContainerCmdModifier { cmd ->
                    cmd.hostConfig?.withDeviceRequests(
                        listOf(
                            DeviceRequest()
                                .withCapabilities(listOf(listOf("gpu")))
                                .withCount(-1)
                        )
                    )
                }
            }
        }

        withExposedPorts(PORT)

        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * Commits the current file system changes in the container into a new image.
     * Should be used for creating an image that contains a loaded model.
     * @param imageName the name of the new image
     */
    fun commitToImage(imageName: String) {
        val dockerImageName = DockerImageName.parse(dockerImageName)
        if (dockerImageName != DockerImageName.parse(imageName)) {
            val dockerClient = DockerClientFactory.instance().client()
            val images = dockerClient.listImagesCmd().withReferenceFilter(imageName).exec()
            if (images.isEmpty()) {
                val imageModel = DockerImageName.parse(imageName)
                dockerClient
                    .commitCmd(containerId)
                    .withRepository(imageModel.unversionedPart)
                    .withLabels(Collections.singletonMap("org.testcontainers.sessionId", ""))
                    .withTag(imageModel.versionPart)
                    .exec()
            }
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

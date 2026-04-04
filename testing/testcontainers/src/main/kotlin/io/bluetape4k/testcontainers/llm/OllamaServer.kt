package io.bluetape4k.testcontainers.llm

import com.github.dockerjava.api.model.DeviceRequest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
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
): GenericContainer<OllamaServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Ollama 서버의 Docker Hub 이미지 이름입니다. */
        const val IMAGE = "ollama/ollama"

        /** 기본 Docker 이미지 태그입니다. */
        const val TAG = "0.5.11"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 식별자입니다. */
        const val NAME = "ollama"

        /** Ollama REST API 포트입니다. */
        const val PORT = 11434

        /**
         * 이미지 이름/태그로 [OllamaServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = OllamaServer(image = "ollama/ollama", tag = "0.5.11")
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 11434 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
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

        /**
         * [DockerImageName]으로 [OllamaServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("ollama/ollama").withTag("0.5.11")
         * val server = OllamaServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름 (`ollama/ollama` 호환 이미지여야 합니다)
         * @param useDefaultPort `true`면 11434 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
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

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
    )

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
     * 현재 컨테이너 파일 시스템 변경 내용을 새로운 이미지로 커밋합니다.
     * 모델이 로드된 상태를 이미지로 저장할 때 사용합니다.
     *
     * ```kotlin
     * val server = OllamaServer()
     * server.start()
     * // 모델 로드 후
     * server.commitToImage("my-ollama-with-model:latest")
     * // 새 이미지가 Docker에 등록됩니다.
     * ```
     *
     * @param imageName 새로 생성할 이미지 이름 (예: `my-ollama:latest`)
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
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Ollama 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val ollama: OllamaServer by lazy {
            OllamaServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

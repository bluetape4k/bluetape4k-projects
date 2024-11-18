package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * Httpbin server
 *
 * 불안정한 [httpbin.org](https://httpbin.org/) 의 API 서버를 Docker를 이용하여 로컬 서버에서 제공합니다.
 *
 * 실제 네트웍을 통한 테스트는 안정적인 [nghttp2.org](https://nghttp2.org/httpbin) 을 사용하는 것을 추천합니다.
 */
class HttpbinServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<HttpbinServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "kong/httpbin"
        const val TAG = "latest"
        const val NAME = "httpbin"
        const val PORT = 80

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinServer {
            return HttpbinServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    init {
        withExposedPorts(PORT)
        withReuse(reuse)

        // 대기전략을 쓰니, port 설정이 안되었다는 예외가 발생한다.
        // waitingFor(Wait.forHttp("/"))

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    /**
     * [HttpbinServer]를 실행해주는 Launcher
     */
    object Launcher {
        val httpbin: HttpbinServer by lazy {
            HttpbinServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

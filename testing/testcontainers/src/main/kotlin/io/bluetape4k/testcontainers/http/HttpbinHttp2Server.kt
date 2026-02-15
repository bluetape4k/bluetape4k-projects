package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * HTTP/2(h2c) 지원 httpbin 테스트 서버입니다.
 *
 * Docker 이미지: `skydoctor/httpbin-http2`
 */
class HttpbinHttp2Server private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<HttpbinHttp2Server>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "skydoctor/httpbin-http2"
        const val TAG = "latest"
        const val NAME = "httpbin-http2"
        const val PORT = 8000

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinHttp2Server {
            return HttpbinHttp2Server(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): HttpbinHttp2Server {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int
        get() = getMappedPort(PORT)

    override val url: String
        get() = "http://$host:$port"

    init {
        withExposedPorts(PORT)
        withReuse(reuse)
        waitingFor(
            Wait.forHttp("/get")
                .forPort(PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(2))
        )

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    /**
     * [HttpbinHttp2Server]를 실행해주는 Launcher
     */
    object Launcher {
        val httpbinHttp2: HttpbinHttp2Server by lazy {
            HttpbinHttp2Server().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

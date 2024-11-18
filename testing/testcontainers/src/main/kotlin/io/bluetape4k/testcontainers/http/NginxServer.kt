package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.NginxContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

/**
 * Nginx를 테스트용으로 사용할 수 있는 컨테이너를 제공한다.
 *
 * 참고: [Nginx](https://hub.docker.com/_/nginx)
 */
class NginxServer private constructor(
    imageName: DockerImageName, useDefaultPort: Boolean, reuse: Boolean,
): NginxContainer<NginxServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "nginx"
        const val TAG = "1.25-alpine"
        const val NAME = "nginx"
        const val PORT = 80

        const val NGINX_PATH = "/usr/share/nginx/html"

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE, tag: String = TAG, useDefaultPort: Boolean = true, reuse: Boolean = true,
        ): NginxServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName, useDefaultPort: Boolean = true, reuse: Boolean = true,
        ): NginxServer {
            return NginxServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    init {
        withExposedPorts(PORT)
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
        fun launch(contentPath: String, useDefaultPort: Boolean = true): NginxServer {
            return NginxServer(useDefaultPort = useDefaultPort).apply {
                withCopyFileToContainer(MountableFile.forHostPath(contentPath), NGINX_PATH)
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

inline fun createNginxServer(
    contentPath: String,
    useDefaultPort: Boolean = true,
    block: NginxServer.() -> Unit,
): NginxServer {
    val nginx = NginxServer(useDefaultPort = useDefaultPort)
        .withCopyFileToContainer(MountableFile.forHostPath(contentPath), NginxServer.NGINX_PATH)
        .apply {
            ShutdownQueue.register(this)
        }
    block(nginx)
    return nginx
}

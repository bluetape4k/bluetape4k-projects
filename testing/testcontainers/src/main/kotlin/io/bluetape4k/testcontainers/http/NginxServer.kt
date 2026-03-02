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
    override val url: String get() = "https://$host:$port"

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

    /**
     * 정적 콘텐츠 경로를 컨테이너에 복사해 실행하는 헬퍼를 제공합니다.
     */
    object Launcher {
        /**
         * 지정한 호스트 디렉터리를 Nginx 기본 문서 경로로 복사해 서버를 시작합니다.
         *
         * ## 동작/계약
         * - 컨테이너 시작 전에 [MountableFile.forHostPath]로 파일을 복사 설정합니다.
         * - 시작 후 [ShutdownQueue]에 종료 작업을 등록합니다.
         */
        fun launch(contentPath: String, useDefaultPort: Boolean = true): NginxServer {
            return NginxServer(useDefaultPort = useDefaultPort).apply {
                withCopyFileToContainer(MountableFile.forHostPath(contentPath), NGINX_PATH)
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

/**
 * 콘텐츠 복사까지 완료된 Nginx 서버를 생성하고 사용자 블록으로 추가 설정을 적용합니다.
 *
 * ## 동작/계약
 * - 서버를 즉시 시작하지 않고 구성된 인스턴스만 반환합니다.
 * - 반환 인스턴스는 [ShutdownQueue]에 종료 작업이 등록됩니다.
 *
 * ```kotlin
 * val nginx = createNginxServer("/tmp/site", true) { start() }
 * // nginx.url.isNotBlank() == true
 * ```
 */
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

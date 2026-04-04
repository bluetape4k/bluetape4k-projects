package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.nginx.NginxContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

/**
 * Nginx를 테스트용으로 사용할 수 있는 컨테이너를 제공한다.
 *
 * ```kotlin
 * val server = NginxServer.Launcher.launch("/path/to/content")
 * // server.url.startsWith("http://") == true
 * ```
 *
 * 참고: [Nginx](https://hub.docker.com/_/nginx)
 */
class NginxServer private constructor(
    imageName: DockerImageName, useDefaultPort: Boolean, reuse: Boolean,
): NginxContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "nginx"
        const val TAG = "1.25-alpine"
        const val NAME = "nginx"
        const val PORT = 80

        const val NGINX_PATH = "/usr/share/nginx/html"

        /**
         * 이미지 이름/태그로 [NginxServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = NginxServer(image = "nginx", tag = "1.25-alpine")
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image          Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag            Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 80 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE, tag: String = TAG, useDefaultPort: Boolean = true, reuse: Boolean = true,
        ): NginxServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [NginxServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("nginx").withTag("1.25-alpine")
         * val server = NginxServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName      Docker 이미지 이름
         * @param useDefaultPort `true`면 80 포트를 고정 바인딩합니다.
         * @param reuse          컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName, useDefaultPort: Boolean = true, reuse: Boolean = true,
        ): NginxServer {
            return NginxServer(imageName, useDefaultPort, reuse)
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
    val nginx: NginxServer = NginxServer(useDefaultPort = useDefaultPort)
        .withCopyFileToContainer(MountableFile.forHostPath(contentPath), NginxServer.NGINX_PATH)
        .apply {
            ShutdownQueue.register(this)
        } as NginxServer
    nginx.block()
    return nginx
}

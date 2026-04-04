package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import io.minio.MinioClient
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.utility.DockerImageName

/**
 * [MinIO](https://min.io) Server 를 Docker container 로 실행해주는 클래스입니다.
 *
 * 참고: [MinIO Docker image](https://hub.docker.com/r/minio/minio/tags)
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class MinIOServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
    username: String = DEFAULT_USER,
    password: String = DEFAULT_PASSWORD,
): MinIOContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "minio/minio"
        const val TAG = "RELEASE.2025-07-23T15-54-02Z"
        const val NAME = "minio"
        const val S3_PORT = 9000
        const val UI_PORT = 9001

        const val DEFAULT_USER = "minioadmin"
        const val DEFAULT_PASSWORD = "minioadmin"

        /**
         * 이미지 이름/태그로 [MinIOServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = MinIOServer(image = "minio/minio", tag = MinIOServer.TAG)
         * // server.url.startsWith("http://") == true (시작 후)
         * ```
         *
         * @param image    Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag      Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 9000/9001 포트를 고정 바인딩합니다.
         * @param reuse    컨테이너 재사용 여부입니다.
         * @param username S3 접속 username (기본: `minioadmin`)
         * @param password S3 접속 password (기본: `minioadmin`)
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            username: String = DEFAULT_USER,
            password: String = DEFAULT_PASSWORD,
        ): MinIOServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse, username, password)
        }

        /**
         * [DockerImageName]으로 [MinIOServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("minio/minio").withTag(MinIOServer.TAG)
         * val server = MinIOServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName Docker 이미지 이름
         * @param useDefaultPort `true`면 9000/9001 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         * @param username S3 접속 username (기본: `minioadmin`)
         * @param password S3 접속 password (기본: `minioadmin`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            username: String = DEFAULT_USER,
            password: String = DEFAULT_PASSWORD,
        ): MinIOServer {
            return MinIOServer(imageName, useDefaultPort, reuse, username, password)
        }
    }

    override val port: Int get() = getMappedPort(S3_PORT)

    override val url: String get() = s3URL

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "username", "password")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "username" to userName,
        "password" to password,
    )

    init {
        withReuse(reuse)
        withUserName(username)
        withPassword(password)

        if (useDefaultPort) {
            exposeCustomPorts(S3_PORT, UI_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 MinIO 서버 싱글턴과 [MinioClient] 팩토리를 제공합니다.
     */
    object Launcher {
        val minio: MinIOServer by lazy {
            MinIOServer().apply {
                start()

                // JVM 종료 시, 자동으로 Close 되도록 합니다
                ShutdownQueue.register(this)
            }
        }

        /**
         * 현재 MinIO 서버 접속 정보로 [MinioClient]를 생성합니다.
         *
         * ```kotlin
         * val client = MinIOServer.Launcher.getClient(MinIOServer.Launcher.minio)
         * // client != null
         * ```
         *
         * @param minio 연결할 [MinIOServer] 인스턴스
         * @return [MinioClient] 인스턴스
         */
        fun getClient(minio: MinIOServer): MinioClient {
            return MinioClient.builder()
                .endpoint(minio.url)
                .credentials(minio.userName, minio.password)
                .build()
        }
    }
}

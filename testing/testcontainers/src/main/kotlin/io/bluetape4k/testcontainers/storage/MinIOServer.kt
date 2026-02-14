package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
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
): MinIOContainer(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "minio/minio"
        const val TAG = "RELEASE.2025-07-23T15-54-02Z"
        const val NAME = "minio"
        const val S3_PORT = 9000
        const val UI_PORT = 9001

        const val DEFAULT_USER = "minioadmin"
        const val DEFAULT_PASSWORD = "minioadmin"

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

        val extraProps = mapOf(
            "username" to userName,
            "password" to password
        )
        writeToSystemProperties(NAME, extraProps)
    }

    object Launcher {
        val minio: MinIOServer by lazy {
            MinIOServer().apply {
                start()

                // JVM 종료 시, 자동으로 Close 되도록 합니다
                ShutdownQueue.register(this)
            }
        }

        fun getClient(minio: MinIOServer): MinioClient {
            return MinioClient.builder()
                .endpoint(minio.url)
                .credentials(minio.userName, minio.password)
                .build()
        }
    }
}

package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.clickhouse.ClickHouseContainer
import org.testcontainers.utility.DockerImageName

/**
 * [ClickHouse](https://clickhouse.com/) Database를 Docker Container로 실행하는 클래스입니다.
 *
 * - 참고: [ClickHouse](https://clickhouse.com/)
 * - 참고: [ClickHouse Docker Hub](https://hub.docker.com/r/clickhouse/clickhouse-server)
 */
class ClickHouseServer private constructor(
    imageName: DockerImageName,
    username: String,
    password: String,
    useDefaultPort: Boolean,
    reuse: Boolean,
): ClickHouseContainer(imageName), JdbcServer {

    companion object: KLogging() {
        const val IMAGE = "clickhouse/clickhouse-server"
        const val TAG = "24"
        const val NAME = "clickhouse"
        const val HTTP_PORT = 8123
        const val NATIVE_PORT = 9000
        const val DATABASE_NAME = "default"
        const val USERNAME = "default"
        const val PASSWORD = ""
        const val DRIVER_CLASS_NAME = "com.clickhouse.jdbc.ClickHouseDriver"

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            username: String = USERNAME,
            password: String = PASSWORD,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ClickHouseServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, username, password, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            username: String = USERNAME,
            password: String = PASSWORD,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ClickHouseServer {
            return ClickHouseServer(imageName, username, password, useDefaultPort, reuse)
        }
    }

    override fun getDriverClassName(): String = DRIVER_CLASS_NAME
    override val port: Int get() = getMappedPort(HTTP_PORT)
    override val url: String get() = jdbcUrl

    // NOTE: 1.19.7의 ClickHouseContainer에서 getDatabaseName() 을 제공하지 않아서 오버라이드함
    // TODO: ClickHouseContainer에 getDatabaseName()을 추가하면 제거할 것
    override fun getDatabaseName(): String {
        return DATABASE_NAME
    }

    init {
        addExposedPorts(HTTP_PORT, NATIVE_PORT)

        withReuse(reuse)
        withUsername(username)
        withPassword(password)
        withDatabaseName(DATABASE_NAME)

        if (useDefaultPort) {
            exposeCustomPorts(HTTP_PORT, NATIVE_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME, buildJdbcProperties())
    }

    object Launcher {
        val clickhouse: ClickHouseServer by lazy {
            ClickHouseServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

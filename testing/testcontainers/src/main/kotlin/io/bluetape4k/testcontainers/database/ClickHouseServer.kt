package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
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
): ClickHouseContainer(imageName), JdbcServer, PropertyExportingServer {

    companion object: KLogging() {
        /** ClickHouse 서버의 Docker Hub 이미지 이름입니다. */
        const val IMAGE = "clickhouse/clickhouse-server"

        /** 기본 Docker 이미지 태그입니다. */
        const val TAG = "25.4"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 식별자입니다. */
        const val NAME = "clickhouse"

        /** ClickHouse HTTP 인터페이스 포트입니다. */
        const val HTTP_PORT = 8123

        /** ClickHouse Native 프로토콜 포트입니다. */
        const val NATIVE_PORT = 9000

        /** 기본 데이터베이스 이름입니다. */
        const val DATABASE_NAME = "default"

        /** 기본 사용자 이름입니다. */
        const val USERNAME = "test"

        /** 기본 비밀번호입니다. */
        const val PASSWORD = "test"

        /** ClickHouse JDBC 드라이버 클래스 이름입니다. */
        const val DRIVER_CLASS_NAME = "com.clickhouse.jdbc.ClickHouseDriver"

        /**
         * [ClickHouseServer]를 생성합니다.
         *
         * @param image             docker image (기본: `clickhouse/clickhouse-server`)
         * @param tag               docker image tag (기본: `25.4`)
         * @param username          사용자 이름 (기본: `test`)
         * @param password          비밀번호 (기본: `test`)
         * @param useDefaultPort    기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse             재사용 여부 (기본: `true`)
         */
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

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "jdbc-url", "driver-class-name", "username", "password", "database-name",
    )

    override fun properties(): Map<String, String> = buildKebabJdbcProperties()

    override fun getDriverClassName(): String = DRIVER_CLASS_NAME
    override val port: Int get() = getMappedPort(HTTP_PORT)
    override val url: String get() = jdbcUrl

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
        writeToSystemProperties()
    }

    /**
     * 테스트 수명주기에서 재사용할 ClickHouse 서버 싱글턴을 제공합니다.
     *
     * ## 동작/계약
     * - 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 작업을 등록합니다.
     * - 이후에는 같은 인스턴스를 재사용합니다.
     */
    object Launcher {
        val clickhouse: ClickHouseServer by lazy {
            ClickHouseServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

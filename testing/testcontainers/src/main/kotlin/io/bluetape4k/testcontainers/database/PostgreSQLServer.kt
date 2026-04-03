package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

/**
 * Docker를 이용하여 Postgres 서버를 실행합니다.
 *
 * [withExtensions]를 사용하여 PostgreSQL contrib 확장(예: `pg_trgm`, `uuid-ossp`, `hstore`)을 활성화할 수 있습니다.
 *
 * ```kotlin
 * PostgreSQLServer()
 *     .withExtensions("pg_trgm", "uuid-ossp")
 *     .apply { start() }
 * ```
 *
 * * 참고: [PostgreSQL](https://hub.docker.com/_/postgres/tags?page=&page_size=&ordering=)
 *
 * @param imageName         Docker image name
 * @param useDefaultPort    default port 사용 여부
 * @param reuse             Container 재사용 여부
 * @param username          Database 사용자 이름
 * @param password          Database 사용자 비밀번호
 */
class PostgreSQLServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
    username: String,
    password: String,
): PostgreSQLContainer(imageName), JdbcServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "postgres"
        const val TAG: String = "18-alpine"
        const val NAME = "postgresql"
        const val PORT = 5432
        const val USERNAME = "test"
        const val PASSWORD = "test"

        const val DRIVER_CLASS_NAME = "org.postgresql.Driver"

        /**
         * [PostgreSQLServer]를 생성합니다.
         *
         * @param image docker image (기본: `postgres`)
         * @param tag docker image tag (기본: `9.6`)
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         * @param username 사용자 이름 (기본: `test`)
         * @param password 비밀번호 (기본: `test`)
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            username: String = USERNAME,
            password: String = PASSWORD,
        ): PostgreSQLServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse, username, password)
        }

        /**
         * [PostgreSQLServer]를 생성합니다.
         *
         * @param imageName docker image name
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         * @param username 사용자 이름 (기본: `test`)
         * @param password 비밀번호 (기본: `test`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            username: String = USERNAME,
            password: String = PASSWORD,
        ): PostgreSQLServer {
            return PostgreSQLServer(
                imageName,
                useDefaultPort,
                reuse,
                username,
                password,
            )
        }
    }

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "jdbc-url", "driver-class-name", "username", "password", "database-name",
    )

    override fun properties(): Map<String, String> = buildKebabJdbcProperties()

    private val extensions = mutableListOf<String>()

    override fun getDriverClassName(): String = DRIVER_CLASS_NAME
    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = jdbcUrl

    init {
        // Docker 의 port 를 노출할 경우에는 이렇게 추가 해주어야 합니다.
        addExposedPorts(PORT)

        withUsername(username)
        withPassword(password)
        withReuse(reuse)

        setWaitStrategy(Wait.forListeningPort())

        // 개발 시에만 사용하면 됩니다.
        // withLogConsumer(Slf4jLogConsumer(log))

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * 컨테이너 시작 시 활성화할 PostgreSQL 확장을 등록합니다.
     *
     * 표준 `postgres` 이미지에 포함된 contrib 확장(예: `pg_trgm`, `uuid-ossp`, `hstore`)을
     * 활성화할 때 사용합니다.
     *
     * @param extensions 활성화할 확장 이름 목록
     * @return this (메서드 체이닝용)
     */
    fun withExtensions(vararg extensions: String): PostgreSQLServer = apply {
        this.extensions.addAll(extensions)
    }

    override fun start() {
        super.start()
        if (extensions.isNotEmpty()) {
            DriverManager.getConnection(jdbcUrl, username, password).use { conn ->
                extensions.distinct().forEach { ext ->
                    conn.createStatement().execute("""CREATE EXTENSION IF NOT EXISTS "$ext"""")
                }
            }
        }
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 PostgreSQL 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val postgres: PostgreSQLServer by lazy {
            PostgreSQLServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * 추가 확장이 활성화된 [PostgreSQLServer] 싱글턴을 생성하고 시작합니다.
         *
         * 표준 `postgres` 이미지에 포함된 contrib 확장(예: `pg_trgm`, `uuid-ossp`, `hstore`)을
         * 활성화할 때 사용합니다.
         *
         * @param extensions 활성화할 확장 이름 목록
         */
        fun withExtensions(vararg extensions: String): PostgreSQLServer =
            PostgreSQLServer().withExtensions(*extensions).apply {
                start()
                ShutdownQueue.register(this)
            }
    }
}

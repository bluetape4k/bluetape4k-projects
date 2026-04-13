package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.cockroachdb.CockroachContainer
import org.testcontainers.utility.DockerImageName

/**
 * 분산형 RDBMS인 CockroachDB를 테스트용으로 사용할 수 있는 컨테이너를 제공한다.
 * Postgres 와 호환됩니다.
 *
 * - 참고: [Cockroach Labs](https://www.cockroachlabs.com/)
 * - 참고: [CockroachDB Docker Hub](https://hub.docker.com/r/cockroachdb/cockroach)
 */
class CockroachServer private constructor(
    imageName: DockerImageName,
    username: String,
    password: String,
    useDefaultPort: Boolean,
    reuse: Boolean,
): CockroachContainer(imageName), JdbcServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "cockroachdb/cockroach"
        const val TAG: String = "v25.4.8"
        const val NAME = "cockroach"
        const val DB_PORT = 26257
        const val REST_API_PORT = 8080
        const val DATABASE_NAME = "defaultdb"
        const val USERNAME = "test"
        const val PASSWORD = "test"

        const val DRIVER_CLASS_NAME = "org.postgresql.Driver"

        /**
         * [CockroachServer]를 생성합니다.
         *
         * ```kotlin
         * val server = CockroachServer(image = "cockroachdb/cockroach", tag = CockroachServer.TAG)
         * // server.url.startsWith("jdbc:postgresql://") == true (시작 후)
         * ```
         *
         * @param image docker image (기본: `cockroachdb/cockroach`)
         * @param tag docker image tag (기본: `v22.2.8`)
         * @param username 사용자 이름 (기본: `test`)
         * @param password 비밀번호 (기본: `test`)
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            username: String = USERNAME,
            password: String = PASSWORD,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): CockroachServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, username, password, useDefaultPort, reuse)
        }

        /**
         * [CockroachServer]를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("cockroachdb/cockroach").withTag(CockroachServer.TAG)
         * val server = CockroachServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName docker image name (eg: `cockroachdb/cockroach:v22.2.8`)
         * @param username 사용자 이름 (기본: `test`)
         * @param password 비밀번호 (기본: `test`)
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            username: String = USERNAME,
            password: String = PASSWORD,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): CockroachServer {
            return CockroachServer(imageName, username, password, useDefaultPort, reuse)
        }
    }

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "jdbc-url", "driver-class-name", "username", "password", "database-name",
    )

    override fun properties(): Map<String, String> = buildKebabJdbcProperties()

    override val port: Int get() = getMappedPort(DB_PORT)
    override val url: String get() = jdbcUrl

    /**
     * JDBC URL을 제공합니다. (기본: "org.postgresql.Driver")
     */
    override fun getDriverClassName(): String = DRIVER_CLASS_NAME

    /** SQL 접속용 Cockroach 포트의 매핑 결과입니다. */
    val dbPort: Int get() = getMappedPort(DB_PORT)

    /** 관리/상태 확인용 REST API 포트의 매핑 결과입니다. */
    val restApiPort: Int get() = getMappedPort(REST_API_PORT)

    init {
        addExposedPorts(REST_API_PORT, DB_PORT)

        withUsername(username)
        withPassword(password)
        withDatabaseName(DATABASE_NAME)

        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(DB_PORT, REST_API_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Cockroach 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val cockroach: CockroachServer by lazy {
            CockroachServer(useDefaultPort = true).apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}

package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

/**
 * Docker를 이용하여 pgvector 확장이 활성화된 PostgreSQL 서버를 실행합니다.
 *
 * `pgvector/pgvector` 이미지를 사용하며, pgvector 벡터 검색 확장이 기본으로 설치됩니다.
 * 벡터 유사도 검색(VECTOR(n) 컬럼 타입, cosine/L2/inner product 거리) 테스트에 사용합니다.
 *
 * `vector` 확장은 컨테이너 시작 시 자동으로 활성화됩니다.
 * [withExtensions]를 사용하여 추가 확장을 활성화할 수 있습니다.
 *
 * ```kotlin
 * PgvectorServer()
 *     .withExtensions("pg_trgm")
 *     .apply { start() }
 * ```
 *
 * * 참고: [pgvector Docker Image](https://hub.docker.com/r/pgvector/pgvector)
 *
 * @param imageName         Docker image name
 * @param useDefaultPort    default port 사용 여부
 * @param reuse             Container 재사용 여부
 * @param username          Database 사용자 이름
 * @param password          Database 사용자 비밀번호
 */
class PgvectorServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
    username: String,
    password: String,
): PostgreSQLContainer(imageName), JdbcServer {

    companion object: KLogging() {
        const val IMAGE = "pgvector/pgvector"
        const val TAG: String = "pg16"
        const val NAME = "pgvector"
        const val PORT = 5432
        const val USERNAME = "test"
        const val PASSWORD = "test"

        const val DRIVER_CLASS_NAME = "org.postgresql.Driver"

        /** pgvector 이미지에 기본 포함된 확장 */
        const val EXTENSION_VECTOR = "vector"

        /**
         * [PgvectorServer]를 생성합니다.
         *
         * @param image docker image (기본: `pgvector/pgvector`)
         * @param tag docker image tag (기본: `pg16`)
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
        ): PgvectorServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse("$image:$tag")
                .asCompatibleSubstituteFor("postgres")
            return invoke(imageName, useDefaultPort, reuse, username, password)
        }

        /**
         * [PgvectorServer]를 생성합니다.
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
        ): PgvectorServer {
            return PgvectorServer(imageName, useDefaultPort, reuse, username, password)
        }
    }

    private val extraExtensions = mutableListOf<String>()

    override fun getDriverClassName(): String = DRIVER_CLASS_NAME
    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = jdbcUrl

    init {
        addExposedPorts(PORT)

        withUsername(username)
        withPassword(password)
        withReuse(reuse)

        setWaitStrategy(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2))

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * 컨테이너 시작 시 활성화할 추가 PostgreSQL 확장을 등록합니다.
     *
     * `vector` 확장은 기본으로 항상 활성화됩니다.
     * 이 메서드로 `pg_trgm` 등 추가 확장을 등록할 수 있습니다.
     *
     * @param extensions 활성화할 확장 이름 목록
     * @return this (메서드 체이닝용)
     */
    fun withExtensions(vararg extensions: String): PgvectorServer = apply {
        extraExtensions.addAll(extensions)
    }

    override fun start() {
        super.start()
        createExtensions(listOf(EXTENSION_VECTOR) + extraExtensions)
        writeToSystemProperties(NAME, buildJdbcProperties())
    }

    private fun createExtensions(extensions: List<String>) {
        DriverManager.getConnection(jdbcUrl, username, password).use { conn ->
            extensions.distinct().forEach { ext ->
                conn.createStatement().execute("""CREATE EXTENSION IF NOT EXISTS "$ext"""")
            }
        }
    }

    /**
     * 테스트에서 재사용할 pgvector 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val pgvector: PgvectorServer by lazy {
            PgvectorServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * 추가 확장이 활성화된 [PgvectorServer] 싱글턴을 생성하고 시작합니다.
         *
         * `vector` 확장은 기본으로 항상 활성화됩니다.
         *
         * @param extensions 추가로 활성화할 확장 이름 목록
         */
        fun withExtensions(vararg extensions: String): PgvectorServer =
            PgvectorServer().withExtensions(*extensions).apply {
                start()
                ShutdownQueue.register(this)
            }
    }
}

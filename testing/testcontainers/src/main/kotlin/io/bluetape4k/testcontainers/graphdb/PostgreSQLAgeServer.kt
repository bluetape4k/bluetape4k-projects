package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.database.JdbcServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * [Apache AGE](https://age.apache.org/) 확장이 설치된 PostgreSQL 테스트 컨테이너를 실행합니다.
 *
 * 컨테이너 시작 시 AGE 확장을 자동으로 초기화합니다:
 * 1. `CREATE EXTENSION IF NOT EXISTS age`
 * 2. `LOAD 'age'`
 * 3. `SET search_path = ag_catalog, "$user", public`
 *
 * ```kotlin
 * val server = PostgreSQLAgeServer()
 * server.start()
 * // server.jdbcUrl, server.username, server.password 사용
 * ```
 *
 * 참고: [Apache AGE Docker Image](https://hub.docker.com/r/apache/age)
 *
 * @param imageName        Docker 이미지 이름
 * @param useDefaultPort   기본 포트 사용 여부
 * @param reuse            컨테이너 재사용 여부
 */
class PostgreSQLAgeServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): PostgreSQLContainer(imageName), JdbcServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Docker 이미지 이름 */
        const val IMAGE = "apache/age"

        /** Docker 이미지 태그 — Apache AGE 1.6.0 for PostgreSQL 17 */
        const val TAG = "release_PG17_1.6.0"

        /** 시스템 프로퍼티 등록에 사용할 서버 이름 */
        const val NAME = "postgresql-age"

        /** PostgreSQL 기본 포트 */
        const val PORT = 5432

        /** JDBC 드라이버 클래스명 */
        const val DRIVER_CLASS_NAME = "org.postgresql.Driver"

        /**
         * [PostgreSQLAgeServer]를 생성합니다.
         *
         * @param imageName Docker 이미지 이름
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): PostgreSQLAgeServer {
            return PostgreSQLAgeServer(imageName, useDefaultPort, reuse)
        }

        /**
         * [PostgreSQLAgeServer]를 생성합니다.
         *
         * @param image Docker 이미지 (기본: `apache/age`)
         * @param tag Docker 이미지 태그 (기본: `release_PG17_1.6.0`)
         * @param useDefaultPort 기본 포트를 사용할지 여부 (기본: `false`)
         * @param reuse 재사용 여부 (기본: `true`)
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): PostgreSQLAgeServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image)
                .withTag(tag)
                .asCompatibleSubstituteFor("postgres")
            return PostgreSQLAgeServer(imageName, useDefaultPort, reuse)
        }
    }

    /** 매핑된 포트 번호 */
    override val port: Int get() = getMappedPort(PORT)

    /** JDBC URL */
    override val url: String get() = super.getJdbcUrl()

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> =
        setOf("host", "port", "url", "jdbc-url", "username", "password", "database")

    override fun properties(): Map<String, String> = buildMap {
        put("host", host)
        put("port", port.toString())
        put("url", url)
        put("jdbc-url", super.getJdbcUrl())
        super.getUsername()?.let { put("username", it) }
        super.getPassword()?.let { put("password", it) }
        super.getDatabaseName()?.let { put("database", it) }
    }

    init {
        addExposedPorts(PORT)

        withDatabaseName("test")
        withUsername("test")
        withPassword("test")
        withReuse(reuse)

        setWaitStrategy(Wait.forListeningPort())

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    /**
     * 컨테이너를 시작하고 AGE 확장을 초기화합니다.
     *
     * AGE 확장 초기화 단계:
     * 1. `CREATE EXTENSION IF NOT EXISTS age` - AGE 확장 생성
     * 2. `LOAD 'age'` - AGE 라이브러리 로드
     * 3. `SET search_path` - `ag_catalog`를 검색 경로에 추가
     */
    override fun start() {
        super.start()

        // AGE extension 초기화
        createConnection("").use { conn ->
            val statements = listOf(
                "CREATE EXTENSION IF NOT EXISTS age",
                "LOAD 'age'",
                """SET search_path = ag_catalog, "${'$'}user", public"""
            )
            statements.forEach { sql ->
                log.info { "AGE 초기화: $sql 실행 중..." }
                conn.createStatement().execute(sql)
            }
        }

        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 PostgreSQL AGE 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        /** PostgreSQL AGE 서버 싱글턴 인스턴스 */
        val postgresqlAge: PostgreSQLAgeServer by lazy {
            PostgreSQLAgeServer().apply {
                start()
                ShutdownQueue.register(this as AutoCloseable)
            }
        }
    }
}

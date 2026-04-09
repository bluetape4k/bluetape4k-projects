package io.bluetape4k.exposed.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import java.time.Duration

/**
 * R2DBC [ConnectionFactoryOptions]를 구성하기 위한 DSL 빌더 클래스입니다.
 *
 * 표준 r2dbc-spi 옵션은 타입-안전한 프로퍼티로, 드라이버별 확장 옵션은
 * [option] 메서드로 추가할 수 있습니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * // H2 인메모리
 * val options = connectionFactoryOptionsOf {
 *     driver = "h2"
 *     protocol = "mem"
 *     database = "test"
 *     option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
 * }
 *
 * // MySQL
 * val options = connectionFactoryOptionsOf {
 *     driver = "mysql"
 *     host = mysql.host
 *     port = mysql.port
 *     user = mysql.username
 *     password = mysql.password
 *     ssl = false
 *     option(Option.valueOf("useServerPrepareStatement"), true)
 * }
 *
 * // PostgreSQL
 * val options = connectionFactoryOptionsOf {
 *     driver = "postgresql"
 *     host = postgres.host
 *     port = postgres.port
 *     user = postgres.username
 *     password = postgres.password
 *     ssl = false
 *     connectTimeout = Duration.ofSeconds(10)
 *     statementTimeout = Duration.ofSeconds(30)
 * }
 * ```
 *
 * @property driver R2DBC 드라이버 이름 (예: "h2", "mysql", "postgresql", "mariadb") — 필수
 * @property protocol 프로토콜 (예: H2 인메모리의 경우 "mem")
 * @property host 데이터베이스 호스트
 * @property port 데이터베이스 포트
 * @property database 데이터베이스(스키마) 이름
 * @property user 접속 사용자 이름
 * @property password 접속 비밀번호
 * @property ssl SSL 사용 여부 (기본값: false)
 * @property connectTimeout 커넥션 생성 타임아웃
 * @property lockWaitTimeout 잠금 대기 타임아웃
 * @property statementTimeout 쿼리 실행 타임아웃
 */
class R2dbcConnectionConfig {

    companion object : KLogging()

    /** R2DBC 드라이버 이름 (예: "h2", "mysql", "postgresql") — 필수 */
    var driver: String = ""

    /** 프로토콜 (예: H2 인메모리의 경우 "mem") */
    var protocol: String? = null

    /** 데이터베이스 호스트 */
    var host: String? = null

    /** 데이터베이스 포트 */
    var port: Int? = null

    /** 데이터베이스(스키마) 이름 */
    var database: String? = null

    /** 접속 사용자 이름 */
    var user: String? = null

    /** 접속 비밀번호 */
    var password: String? = null

    /** SSL 사용 여부 (기본값: false) */
    var ssl: Boolean = false

    /** 커넥션 생성 타임아웃 */
    var connectTimeout: Duration? = null

    /** 잠금 대기 타임아웃 */
    var lockWaitTimeout: Duration? = null

    /** 쿼리 실행 타임아웃 */
    var statementTimeout: Duration? = null

    private val extraOptions = mutableListOf<Pair<Option<*>, Any>>()

    /**
     * 드라이버별 확장 옵션을 추가합니다.
     *
     * ## 사용 예
     *
     * ```kotlin
     * option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
     * option(Option.valueOf("useServerPrepareStatement"), true)
     * option(PostgresqlConnectionFactoryProvider.PREPARED_STATEMENT_CACHE_QUERIES, 256)
     * ```
     *
     * @param key r2dbc-spi [Option] 키
     * @param value 옵션 값
     */
    fun <T : Any> option(key: Option<T>, value: T) {
        extraOptions += key to value
    }

    /**
     * 설정된 값으로 [ConnectionFactoryOptions]를 생성합니다.
     *
     * @throws IllegalArgumentException [driver]가 공백인 경우
     */
    fun build(): ConnectionFactoryOptions {
        driver.requireNotBlank("driver")

        return ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, driver)
            .apply { protocol?.let { option(ConnectionFactoryOptions.PROTOCOL, it) } }
            .apply { host?.let { option(ConnectionFactoryOptions.HOST, it) } }
            .apply { port?.let { option(ConnectionFactoryOptions.PORT, it) } }
            .apply { database?.let { option(ConnectionFactoryOptions.DATABASE, it) } }
            .apply { user?.let { option(ConnectionFactoryOptions.USER, it) } }
            .apply { password?.let { option(ConnectionFactoryOptions.PASSWORD, it) } }
            .option(ConnectionFactoryOptions.SSL, ssl)
            .apply { connectTimeout?.let { option(ConnectionFactoryOptions.CONNECT_TIMEOUT, it) } }
            .apply { lockWaitTimeout?.let { option(ConnectionFactoryOptions.LOCK_WAIT_TIMEOUT, it) } }
            .apply { statementTimeout?.let { option(ConnectionFactoryOptions.STATEMENT_TIMEOUT, it) } }
            .apply {
                @Suppress("UNCHECKED_CAST")
                extraOptions.forEach { (key, value) ->
                    option(key as Option<Any>, value)
                }
            }
            .build()
    }
}

/**
 * DSL 람다로 [ConnectionFactoryOptions]를 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * // H2 인메모리
 * val options = connectionFactoryOptionsOf {
 *     driver = "h2"
 *     protocol = "mem"
 *     database = "test"
 *     option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
 * }
 *
 * // MySQL
 * val options = connectionFactoryOptionsOf {
 *     driver = "mysql"
 *     host = "localhost"
 *     port = 3306
 *     user = "root"
 *     password = "secret"
 *     ssl = false
 * }
 * ```
 *
 * @param init [R2dbcConnectionConfig] 설정 람다
 * @return 생성된 [ConnectionFactoryOptions]
 */
inline fun connectionFactoryOptionsOf(
    init: R2dbcConnectionConfig.() -> Unit,
): ConnectionFactoryOptions = R2dbcConnectionConfig().apply(init).build()

/**
 * DSL 람다로 [ConnectionFactory]를 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val factory = connectionFactoryOf {
 *     driver = "postgresql"
 *     host = "localhost"
 *     port = 5432
 *     database = "mydb"
 *     user = "user"
 *     password = "secret"
 * }
 * ```
 *
 * @param init [R2dbcConnectionConfig] 설정 람다
 * @return 생성된 [ConnectionFactory]
 */
inline fun connectionFactoryOf(
    init: R2dbcConnectionConfig.() -> Unit,
): ConnectionFactory = ConnectionFactories.get(connectionFactoryOptionsOf(init))

/**
 * R2DBC URL 문자열로 [ConnectionFactoryOptions]를 생성합니다.
 *
 * R2DBC URL 형식: `r2dbc:<driver>[+<protocol>]://[<user>:<password>@]<host>[:<port>][/<database>]`
 *
 * ## 사용 예
 *
 * ```kotlin
 * // H2 인메모리
 * val options = connectionFactoryOptionsOf("r2dbc:h2:mem:///testdb")
 *
 * // MySQL
 * val options = connectionFactoryOptionsOf("r2dbc:mysql://user:secret@localhost:3306/mydb")
 *
 * // PostgreSQL
 * val options = connectionFactoryOptionsOf("r2dbc:postgresql://user:secret@localhost:5432/mydb")
 * ```
 *
 * @param url R2DBC URL 문자열
 * @return 파싱된 [ConnectionFactoryOptions]
 */
fun connectionFactoryOptionsOf(url: String): ConnectionFactoryOptions =
    ConnectionFactoryOptions.parse(url)

/**
 * R2DBC URL 문자열로 [ConnectionFactory]를 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val factory = connectionFactoryOf("r2dbc:postgresql://user:secret@localhost:5432/mydb")
 * ```
 *
 * @param url R2DBC URL 문자열
 * @return 생성된 [ConnectionFactory]
 */
fun connectionFactoryOf(url: String): ConnectionFactory =
    ConnectionFactories.get(url)

package io.bluetape4k.exposed.r2dbc.pool

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions

/**
 * 연결 설정([R2dbcConnectionConfig])과 풀 설정([R2dbcPoolConfig])을
 * 한 번에 구성하는 통합 DSL 클래스입니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = r2dbcConnectionPool {
 *     connection {
 *         driver = "postgresql"
 *         host = "localhost"
 *         port = 5432
 *         database = "mydb"
 *         user = "user"
 *         password = "secret"
 *     }
 *     pool {
 *         maxSize = 50
 *         initialSize = 5
 *         minIdle = 5
 *         maxIdleTime = Duration.ofMinutes(5)
 *     }
 * }
 * ```
 */
class R2dbcConfig {
    val connection: R2dbcConnectionConfig = R2dbcConnectionConfig()
    val pool: R2dbcPoolConfig = R2dbcPoolConfig()

    /** 연결 옵션을 DSL 람다로 설정합니다. */
    fun connection(init: R2dbcConnectionConfig.() -> Unit) {
        connection.apply(init)
    }

    /** 커넥션 풀 옵션을 DSL 람다로 설정합니다. */
    fun pool(init: R2dbcPoolConfig.() -> Unit) {
        pool.apply(init)
    }
}

/**
 * [R2dbcConfig] DSL로 연결 설정과 풀 설정을 한 번에 구성하여 [ConnectionPool]을 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = r2dbcConnectionPool {
 *     connection {
 *         driver = "postgresql"
 *         host = postgres.host
 *         port = postgres.port
 *         user = postgres.username
 *         password = postgres.password
 *     }
 *     pool {
 *         maxSize = 50
 *         initialSize = 5
 *     }
 * }
 * ```
 *
 * @param init [R2dbcConfig] 설정 람다
 * @return 생성된 [ConnectionPool]
 */
inline fun r2dbcConnectionPool(
    init: R2dbcConfig.() -> Unit,
): ConnectionPool {
    val config = R2dbcConfig().apply(init)
    return connectionPoolOf(config.connection.build(), config.pool)
}

/**
 * R2DBC URL 문자열과 [R2dbcPoolConfig]로 [ConnectionPool]을 생성합니다.
 *
 * R2DBC URL 형식: `r2dbc:<driver>[+<protocol>]://[<user>:<password>@]<host>[:<port>][/<database>]`
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = r2dbcConnectionPool("r2dbc:postgresql://user:secret@localhost:5432/mydb") {
 *     maxSize = 50
 *     initialSize = 5
 *     maxIdleTime = Duration.ofMinutes(5)
 * }
 * ```
 *
 * @param url R2DBC URL 문자열
 * @param init [R2dbcPoolConfig] 설정 람다 (생략 시 기본값 사용)
 * @return 생성된 [ConnectionPool]
 */
inline fun r2dbcConnectionPool(
    url: String,
    init: R2dbcPoolConfig.() -> Unit = {},
): ConnectionPool = connectionPoolOf(connectionFactoryOptionsOf(url), R2dbcPoolConfig().apply(init))

/**
 * [ConnectionFactoryOptions]와 [R2dbcPoolConfig]로 [ConnectionPool]을 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = connectionPoolOf(connectionFactoryOptions, R2dbcPoolConfig(maxSize = 50))
 * ```
 *
 * @param connectionFactoryOptions R2DBC 커넥션 팩토리 옵션
 * @param poolConfig 커넥션 풀 설정 (기본값: [R2dbcPoolConfig] 기본 설정)
 * @return 생성된 [ConnectionPool]
 */
fun connectionPoolOf(
    connectionFactoryOptions: ConnectionFactoryOptions,
    poolConfig: R2dbcPoolConfig = R2dbcPoolConfig(),
): ConnectionPool {
    val connectionFactory = ConnectionFactories.get(connectionFactoryOptions)
    return connectionPoolOf(connectionFactory, poolConfig)
}

/**
 * [ConnectionFactoryOptions]와 DSL 람다로 [ConnectionPool]을 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = connectionPoolOf(connectionFactoryOptions) {
 *     maxSize = 50
 *     initialSize = 5
 *     minIdle = 5
 *     maxIdleTime = Duration.ofMinutes(5)
 * }
 * ```
 *
 * @param connectionFactoryOptions R2DBC 커넥션 팩토리 옵션
 * @param init [R2dbcPoolConfig] 설정 람다
 * @return 생성된 [ConnectionPool]
 */
inline fun connectionPoolOf(
    connectionFactoryOptions: ConnectionFactoryOptions,
    init: R2dbcPoolConfig.() -> Unit,
): ConnectionPool = connectionPoolOf(connectionFactoryOptions, R2dbcPoolConfig().apply(init))

/**
 * [ConnectionFactory]와 [R2dbcPoolConfig]로 [ConnectionPool]을 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = connectionPoolOf(connectionFactory, R2dbcPoolConfig(maxSize = 50))
 * ```
 *
 * @param connectionFactory R2DBC 커넥션 팩토리
 * @param poolConfig 커넥션 풀 설정 (기본값: [R2dbcPoolConfig] 기본 설정)
 * @return 생성된 [ConnectionPool]
 */
fun connectionPoolOf(
    connectionFactory: ConnectionFactory,
    poolConfig: R2dbcPoolConfig = R2dbcPoolConfig(),
): ConnectionPool {
    val config = poolConfig.toConnectionPoolConfiguration(connectionFactory)
    return ConnectionPool(config)
}

/**
 * [ConnectionFactory]와 DSL 람다로 [ConnectionPool]을 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = connectionPoolOf(connectionFactory) {
 *     maxSize = 100
 *     acquireRetry = 5
 *     backgroundEvictionInterval = Duration.ofSeconds(30)
 * }
 * ```
 *
 * @param connectionFactory R2DBC 커넥션 팩토리
 * @param init [R2dbcPoolConfig] 설정 람다
 * @return 생성된 [ConnectionPool]
 */
inline fun connectionPoolOf(
    connectionFactory: ConnectionFactory,
    init: R2dbcPoolConfig.() -> Unit,
): ConnectionPool = connectionPoolOf(connectionFactory, R2dbcPoolConfig().apply(init))

/**
 * [ConnectionFactoryOptions]에서 [ConnectionPool]로 변환하는 확장 함수입니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = connectionFactoryOptions.toConnectionPool(R2dbcPoolConfig(maxSize = 50))
 * ```
 *
 * @param poolConfig 커넥션 풀 설정 (기본값: [R2dbcPoolConfig] 기본 설정)
 * @return 생성된 [ConnectionPool]
 */
fun ConnectionFactoryOptions.toConnectionPool(
    poolConfig: R2dbcPoolConfig = R2dbcPoolConfig(),
): ConnectionPool = connectionPoolOf(this, poolConfig)

/**
 * [ConnectionFactoryOptions]에서 DSL 람다로 [ConnectionPool]을 생성하는 확장 함수입니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val pool = connectionFactoryOptions.toConnectionPool {
 *     maxSize = 50
 *     initialSize = 5
 * }
 * ```
 *
 * @param init [R2dbcPoolConfig] 설정 람다
 * @return 생성된 [ConnectionPool]
 */
inline fun ConnectionFactoryOptions.toConnectionPool(
    init: R2dbcPoolConfig.() -> Unit,
): ConnectionPool = connectionPoolOf(this, init)

/**
 * [R2dbcPoolConfig]를 [ConnectionPoolConfiguration]으로 변환합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val poolConfig = R2dbcPoolConfig(maxSize = 50)
 * val configuration = poolConfig.toConnectionPoolConfiguration(connectionFactory)
 * val pool = ConnectionPool(configuration)
 * ```
 *
 * @param connectionFactory R2DBC 커넥션 팩토리
 * @return [ConnectionPoolConfiguration] 인스턴스
 */
fun R2dbcPoolConfig.toConnectionPoolConfiguration(
    connectionFactory: ConnectionFactory,
): ConnectionPoolConfiguration =
    ConnectionPoolConfiguration.builder(connectionFactory)
        .maxIdleTime(maxIdleTime)
        .maxLifeTime(maxLifeTime)
        .maxCreateConnectionTime(maxCreateConnectionTime)
        .maxSize(maxSize)
        .initialSize(initialSize)
        .minIdle(minIdle)
        .acquireRetry(acquireRetry)
        .backgroundEvictionInterval(backgroundEvictionInterval)
        .maxAcquireTime(maxAcquireTime)
        .build()

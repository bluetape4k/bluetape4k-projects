package io.bluetape4k.examples.exposed.webflux.config

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.r2dbc.pool.connectionFactoryOptionsOf
import io.bluetape4k.r2dbc.pool.connectionPoolOf
import io.bluetape4k.utils.Runtimex
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * R2DBC 커넥션 풀 설정입니다.
 *
 * `application.yml` / `application.properties`에서 `bluetape4k.r2dbc.pool.*` 프로퍼티로 재정의할 수 있습니다.
 *
 * ```yaml
 * bluetape4k:
 *   r2dbc:
 *     pool:
 *       max-idle-time: 10m
 *       max-life-time: 30m
 *       max-create-connection-time: 10s
 *       max-size: 64
 *       initial-size: 8
 *       min-idle: 8
 *       acquire-retry: 3
 *       background-eviction-interval: 1m
 *       max-acquire-time: 10s
 * ```
 */
@ConfigurationProperties(prefix = "bluetape4k.r2dbc.pool")
data class R2dbcPoolProperties(
    val maxIdleTime: Duration = Duration.ofMinutes(10),
    val maxLifeTime: Duration = Duration.ofMinutes(30),
    val maxCreateConnectionTime: Duration = Duration.ofSeconds(10),
    val maxSize: Int = maxOf(Runtimex.availableProcessors * 8, 64),
    val initialSize: Int = 8,
    val minIdle: Int = 8,
    val acquireRetry: Int = 3,
    val backgroundEvictionInterval: Duration = Duration.ofMinutes(1),
    val maxAcquireTime: Duration = Duration.ofSeconds(10),
)

/**
 * WebFlux 데모에서 사용할 Exposed R2DBC 데이터베이스를 구성합니다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(R2dbcPoolProperties::class)
class ExposedR2dbcConfig {

    companion object: KLoggingChannel()

    /**
     * Java 21 가상 스레드 기반 코루틴 컨텍스트를 제공합니다.
     * 데이터베이스 I/O 작업에 최적화되어 있습니다.
     */
    @Bean(destroyMethod = "")
    fun databaseCoroutineDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO // 기본적으로 IO 디스패처를 사용합니다.
        // return Dispatchers.VT // Java 21 가상 스레드 사용
        // return Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
    }

    @Bean
    fun connectionFactoryOptions(
        @Value("\${spring.r2dbc.url}") url: String,
        @Value("\${spring.r2dbc.username:}") username: String,
        @Value("\${spring.r2dbc.password:}") password: String,
    ): ConnectionFactoryOptions {
        return connectionFactoryOptionsOf(url).mutate().apply {
            option(ConnectionFactoryOptions.USER, username)
            option(ConnectionFactoryOptions.PASSWORD, password)
        }.build()
    }

    @Bean
    fun connectionPool(
        connectionFactoryOptions: ConnectionFactoryOptions,
        poolProperties: R2dbcPoolProperties,
    ): ConnectionPool {
        return connectionPoolOf(connectionFactoryOptions) {
            maxIdleTime = poolProperties.maxIdleTime
            maxLifeTime = poolProperties.maxLifeTime
            maxCreateConnectionTime = poolProperties.maxCreateConnectionTime
            maxSize = poolProperties.maxSize
            initialSize = poolProperties.initialSize
            minIdle = poolProperties.minIdle
            acquireRetry = poolProperties.acquireRetry
            backgroundEvictionInterval = poolProperties.backgroundEvictionInterval
            maxAcquireTime = poolProperties.maxAcquireTime
        }
    }

    /**
     * Exposed `suspendTransaction` 호출에서 사용할 기본 R2DBC 데이터베이스 인스턴스입니다.
     */
    @Bean
    fun r2dbcDatabase(
        connectionPool: ConnectionPool,
        connectionFactoryOptions: ConnectionFactoryOptions,
        databaseCoroutineDispatcher: CoroutineDispatcher,
    ): R2dbcDatabase {
        val config = R2dbcDatabaseConfig {
            this.connectionFactoryOptions = connectionFactoryOptions
            this.dispatcher = databaseCoroutineDispatcher
        }

        log.info { "R2DBC Database 설정 완료 (connectionPool 기반). config=$config" }
        return R2dbcDatabase.connect(connectionPool, databaseConfig = config)
    }
}

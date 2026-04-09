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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * WebFlux 데모에서 사용할 Exposed R2DBC 데이터베이스를 구성합니다.
 */
@Configuration(proxyBeanMethods = false)
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
    fun connectionPool(connectionFactoryOptions: ConnectionFactoryOptions): ConnectionPool {
        return connectionPoolOf(connectionFactoryOptions) {
            maxIdleTime = Duration.ofMinutes(10)
            maxLifeTime = Duration.ofMinutes(30)
            maxCreateConnectionTime = Duration.ofSeconds(10)
            maxSize = maxOf(Runtimex.availableProcessors * 8, 64)
            initialSize = 8
            minIdle = 8
            acquireRetry = 3
            backgroundEvictionInterval = Duration.ofMinutes(1)
            maxAcquireTime = Duration.ofSeconds(10)
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

package io.bluetape4k.exposed.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration

class ConnectionPoolSupportTest {

    companion object : KLogging()

    private fun h2ConnectionFactoryOptions(): ConnectionFactoryOptions =
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "h2")
            .option(ConnectionFactoryOptions.PROTOCOL, "mem")
            .option(ConnectionFactoryOptions.DATABASE, "pool_test_${System.nanoTime()}")
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()

    @Test
    fun `기본 설정으로 R2dbcPoolConfig 생성`() {
        val config = R2dbcPoolConfig()

        config.maxSize shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_SIZE
        config.initialSize shouldBeEqualTo R2dbcPoolConfig.DEFAULT_INITIAL_SIZE
        config.minIdle shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MIN_IDLE
        config.acquireRetry shouldBeEqualTo R2dbcPoolConfig.DEFAULT_ACQUIRE_RETRY
        config.maxIdleTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_IDLE_TIME
        config.maxLifeTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_LIFE_TIME
        config.maxCreateConnectionTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_CREATE_CONNECTION_TIME
        config.backgroundEvictionInterval shouldBeEqualTo R2dbcPoolConfig.DEFAULT_BACKGROUND_EVICTION_INTERVAL
        config.maxAcquireTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_ACQUIRE_TIME

        log.debug { "기본 R2dbcPoolConfig: $config" }
    }

    @Test
    fun `DSL 람다로 R2dbcPoolConfig 커스터마이즈`() {
        val config = R2dbcPoolConfig().apply {
            maxSize = 50
            initialSize = 5
            minIdle = 5
            acquireRetry = 5
            maxIdleTime = Duration.ofMinutes(5)
            maxLifeTime = Duration.ofMinutes(20)
        }

        config.maxSize shouldBeEqualTo 50
        config.initialSize shouldBeEqualTo 5
        config.minIdle shouldBeEqualTo 5
        config.acquireRetry shouldBeEqualTo 5
        config.maxIdleTime shouldBeEqualTo Duration.ofMinutes(5)
        config.maxLifeTime shouldBeEqualTo Duration.ofMinutes(20)
    }

    @Test
    fun `ConnectionFactoryOptions 로 ConnectionPool 생성 - 기본 설정`() {
        val options = h2ConnectionFactoryOptions()
        val pool = connectionPoolOf(options)

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()

        pool.close()
    }

    @Test
    fun `ConnectionFactoryOptions 와 R2dbcPoolConfig 로 ConnectionPool 생성`() {
        val options = h2ConnectionFactoryOptions()
        val poolConfig = R2dbcPoolConfig(
            maxSize = 20,
            initialSize = 4,
            minIdle = 2,
        )
        val pool = connectionPoolOf(options, poolConfig)

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()

        pool.close()
    }

    @Test
    fun `ConnectionFactoryOptions 와 DSL 람다로 ConnectionPool 생성`() {
        val options = h2ConnectionFactoryOptions()
        val pool = connectionPoolOf(options) {
            maxSize = 30
            initialSize = 4
            minIdle = 2
            maxIdleTime = Duration.ofMinutes(5)
            maxAcquireTime = Duration.ofSeconds(5)
        }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()

        log.debug { "ConnectionPool 생성 완료. isDisposed=${pool.isDisposed}" }
        pool.isDisposed.shouldBeFalse()

        pool.close()
    }

    @Test
    fun `toConnectionPool 확장 함수로 ConnectionPool 생성`() {
        val options = h2ConnectionFactoryOptions()
        val pool = options.toConnectionPool {
            maxSize = 25
            initialSize = 3
        }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()

        pool.close()
    }

    @Test
    fun `R2dbcPoolConfig copy 로 설정 변경`() {
        val base = R2dbcPoolConfig()
        val customized = base.copy(maxSize = 200, initialSize = 20)

        customized.maxSize shouldBeEqualTo 200
        customized.initialSize shouldBeEqualTo 20
        // 변경하지 않은 필드는 기본값 유지
        customized.minIdle shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MIN_IDLE
        customized.acquireRetry shouldBeEqualTo R2dbcPoolConfig.DEFAULT_ACQUIRE_RETRY
    }

    @Test
    fun `acquireRetry 0 허용 - 재시도 없음`() {
        val config = R2dbcPoolConfig(acquireRetry = 0)
        config.acquireRetry shouldBeEqualTo 0

        val options = h2ConnectionFactoryOptions()
        val pool = connectionPoolOf(options, config)
        pool.shouldNotBeNull()
        pool.close()
    }
}

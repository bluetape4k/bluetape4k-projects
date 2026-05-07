package io.bluetape4k.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import io.r2dbc.spi.ValidationDepth
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.test.assertFailsWith

class ConnectionPoolSupportTest {

    companion object : KLogging()

    private fun h2ConnectionFactoryOptions(): ConnectionFactoryOptions =
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "h2")
            .option(ConnectionFactoryOptions.PROTOCOL, "mem")
            .option(ConnectionFactoryOptions.DATABASE, "pool_test_${System.nanoTime()}")
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()

    private fun h2ConnectionFactory() =
        ConnectionFactories.get(h2ConnectionFactoryOptions())

    @Suppress("UNCHECKED_CAST")
    private fun <T> ConnectionPoolConfiguration.readConfigValue(methodName: String): T {
        val method = javaClass.getDeclaredMethod(methodName).apply {
            isAccessible = true
        }
        return method.invoke(this) as T
    }

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
        config.maxPendingAcquire shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_PENDING_ACQUIRE
        config.maxValidationTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_VALIDATION_TIME
        config.validationDepth shouldBeEqualTo R2dbcPoolConfig.DEFAULT_VALIDATION_DEPTH
        config.validationQuery.shouldBeNull()
        config.poolName.shouldBeNull()
        config.registerJmx.shouldBeFalse()

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
            maxPendingAcquire = 128
            maxValidationTime = Duration.ofSeconds(1)
            validationDepth = ValidationDepth.REMOTE
            validationQuery = "SELECT 1"
            poolName = "r2dbc-main"
            registerJmx = true
        }

        config.maxSize shouldBeEqualTo 50
        config.initialSize shouldBeEqualTo 5
        config.minIdle shouldBeEqualTo 5
        config.acquireRetry shouldBeEqualTo 5
        config.maxIdleTime shouldBeEqualTo Duration.ofMinutes(5)
        config.maxLifeTime shouldBeEqualTo Duration.ofMinutes(20)
        config.maxPendingAcquire shouldBeEqualTo 128
        config.maxValidationTime shouldBeEqualTo Duration.ofSeconds(1)
        config.validationDepth shouldBeEqualTo ValidationDepth.REMOTE
        config.validationQuery shouldBeEqualTo "SELECT 1"
        config.poolName shouldBeEqualTo "r2dbc-main"
        config.registerJmx shouldBeEqualTo true
    }

    @Test
    fun `고처리량 프리셋은 워밍업과 빠른 검증 설정을 제공한다`() {
        val config = R2dbcPoolConfig.highThroughput(maxSize = 64, poolName = "exposed-r2dbc")

        config.maxSize shouldBeEqualTo 64
        config.initialSize shouldBeEqualTo minOf(64, R2dbcPoolConfig.HIGH_THROUGHPUT_WARMUP_SIZE)
        config.minIdle shouldBeEqualTo config.initialSize
        config.acquireRetry shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_ACQUIRE_RETRY
        config.maxPendingAcquire shouldBeEqualTo 256
        config.maxIdleTime shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_MAX_IDLE_TIME
        config.maxCreateConnectionTime shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_MAX_CREATE_CONNECTION_TIME
        config.backgroundEvictionInterval shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_BACKGROUND_EVICTION_INTERVAL
        config.maxValidationTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_VALIDATION_TIME
        config.validationDepth shouldBeEqualTo ValidationDepth.LOCAL
        config.validationQuery.shouldBeNull()
        config.poolName shouldBeEqualTo "exposed-r2dbc"
        config.registerJmx.shouldBeFalse()
    }

    @Test
    fun `R2dbcPoolConfig 를 ConnectionPoolConfiguration 으로 변환할 때 고급 옵션을 반영한다`() {
        val poolConfig = R2dbcPoolConfig(
            maxSize = 16,
            initialSize = 4,
            minIdle = 4,
            acquireRetry = 1,
            maxIdleTime = Duration.ofMinutes(5),
            maxLifeTime = Duration.ofMinutes(30),
            maxCreateConnectionTime = Duration.ofSeconds(5),
            backgroundEvictionInterval = Duration.ofSeconds(30),
            maxAcquireTime = Duration.ofSeconds(2),
            maxPendingAcquire = 64,
            maxValidationTime = Duration.ofSeconds(1),
            validationDepth = ValidationDepth.REMOTE,
            validationQuery = "SELECT 1",
            poolName = "r2dbc-orders",
            registerJmx = true,
        )

        val configuration = poolConfig.toConnectionPoolConfiguration(h2ConnectionFactory())

        configuration.readConfigValue<Int>("getMaxSize") shouldBeEqualTo 16
        configuration.readConfigValue<Int>("getInitialSize") shouldBeEqualTo 4
        configuration.readConfigValue<Int>("getMinIdle") shouldBeEqualTo 4
        configuration.readConfigValue<Int>("getAcquireRetry") shouldBeEqualTo 1
        configuration.readConfigValue<Duration>("getMaxValidationTime") shouldBeEqualTo Duration.ofSeconds(1)
        configuration.readConfigValue<ValidationDepth>("getValidationDepth") shouldBeEqualTo ValidationDepth.REMOTE
        configuration.readConfigValue<String>("getValidationQuery") shouldBeEqualTo "SELECT 1"
        configuration.readConfigValue<String>("getName") shouldBeEqualTo "r2dbc-orders"
        configuration.readConfigValue<Boolean>("isRegisterJmx") shouldBeEqualTo true
    }

    @Test
    fun `initialSize 0 허용 - 커넥션 지연 생성`() {
        val config = R2dbcPoolConfig(initialSize = 0, minIdle = 0)

        config.initialSize shouldBeEqualTo 0
        config.minIdle shouldBeEqualTo 0

        val pool = connectionPoolOf(h2ConnectionFactoryOptions(), config)
        pool.shouldNotBeNull()
        pool.close()
    }

    @Test
    fun `잘못된 풀 크기 설정은 즉시 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(maxSize = 4, initialSize = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(maxSize = 4, minIdle = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(registerJmx = true)
        }
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(maxPendingAcquire = -2)
        }
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(validationQuery = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(poolName = " ")
        }
    }

    @Test
    fun `DSL 변경 후 잘못된 풀 크기 설정도 변환 시점에 거부한다`() {
        val config = R2dbcPoolConfig().apply {
            initialSize = maxSize + 1
        }

        assertFailsWith<IllegalArgumentException> {
            config.toConnectionPoolConfiguration(h2ConnectionFactory())
        }
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
    fun `ConnectionPool 에서 코루틴 bridge 로 커넥션 획득 후 반납`() {
        runBlocking {
            val options = connectionFactoryOptionsOf("r2dbc:h2:mem:///pool_acquire_test;DB_CLOSE_DELAY=-1")
            val pool = connectionPoolOf(
                options,
                R2dbcPoolConfig(
                    maxSize = 8,
                    initialSize = 0,
                    minIdle = 0,
                    maxValidationTime = Duration.ofSeconds(1),
                    validationQuery = "SELECT 1",
                )
            )

            try {
                withTimeout(3_000) {
                    val connection = pool.create().awaitSingle()
                    connection.metadata.databaseProductName shouldBeEqualTo "H2"
                    connection.close().awaitFirstOrNull()
                }
            } finally {
                pool.close()
            }
        }
    }

    @Test
    fun `maxPendingAcquire 제한은 풀 포화 시 추가 획득을 빠르게 거부한다`() {
        val options = connectionFactoryOptionsOf("r2dbc:h2:mem:///pool_pending_test;DB_CLOSE_DELAY=-1")
        val pool = connectionPoolOf(
            options,
            R2dbcPoolConfig(
                maxSize = 1,
                initialSize = 1,
                minIdle = 1,
                maxPendingAcquire = 0,
                maxAcquireTime = Duration.ofMillis(50),
                maxValidationTime = Duration.ofSeconds(1),
                validationQuery = "SELECT 1",
            )
        )

        pool.warmup().block(Duration.ofSeconds(3))
        val first = Mono.from(pool.create()).block(Duration.ofSeconds(3))
        try {
            val failure = runCatching {
                Mono.from(pool.create()).block(Duration.ofSeconds(3))
            }.exceptionOrNull()

            failure.shouldNotBeNull()
        } finally {
            first.shouldNotBeNull()
            Mono.from(first.close()).block(Duration.ofSeconds(3))
            pool.close()
        }
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

package io.bluetape4k.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.Option
import io.r2dbc.spi.ValidationDepth
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class R2dbcPoolConfigTest {

    companion object : KLogging()

    private fun h2DbName() = "pool_cfg_${System.nanoTime()}"

    /**
     * DSL 람다로 H2 인메모리 ConnectionFactory 생성 (올바른 API)
     */
    private fun h2Factory(dbName: String = h2DbName()) = connectionFactoryOf {
        driver = "h2"
        protocol = "mem"
        database = dbName
        option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
    }

    /**
     * H2 인메모리 ConnectionFactoryOptions 생성
     */
    private fun h2Options(dbName: String = h2DbName()) = connectionFactoryOptionsOf {
        driver = "h2"
        protocol = "mem"
        database = dbName
        option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
    }

    // ─── 기본값 검증 ────────────────────────────────────────────────

    @Test
    fun `기본 설정으로 생성 시 DEFAULT 값이 적용된다`() {
        val config = R2dbcPoolConfig()

        config.maxIdleTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_IDLE_TIME
        config.maxLifeTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_LIFE_TIME
        config.maxCreateConnectionTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_CREATE_CONNECTION_TIME
        config.maxSize shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_SIZE
        config.initialSize shouldBeEqualTo R2dbcPoolConfig.DEFAULT_INITIAL_SIZE
        config.minIdle shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MIN_IDLE
        config.acquireRetry shouldBeEqualTo R2dbcPoolConfig.DEFAULT_ACQUIRE_RETRY
        config.backgroundEvictionInterval shouldBeEqualTo R2dbcPoolConfig.DEFAULT_BACKGROUND_EVICTION_INTERVAL
        config.maxAcquireTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_ACQUIRE_TIME
        config.maxPendingAcquire shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_PENDING_ACQUIRE
        config.maxValidationTime shouldBeEqualTo R2dbcPoolConfig.DEFAULT_MAX_VALIDATION_TIME
        config.validationDepth shouldBeEqualTo R2dbcPoolConfig.DEFAULT_VALIDATION_DEPTH
        config.validationQuery.shouldBeNull()
        config.poolName.shouldBeNull()
        config.registerJmx.shouldBeFalse()
    }

    @Test
    fun `DEFAULT_MAX_SIZE 는 양수여야 한다`() {
        R2dbcPoolConfig.DEFAULT_MAX_SIZE shouldBeGreaterThan 0
    }

    @Test
    fun `DEFAULT_VALIDATION_DEPTH 는 LOCAL 이다`() {
        R2dbcPoolConfig.DEFAULT_VALIDATION_DEPTH shouldBeEqualTo ValidationDepth.LOCAL
    }

    // ─── validate 제약 ────────────────────────────────────────────────

    @Test
    fun `maxSize 가 0 이하이면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(maxSize = 0)
        }
    }

    @Test
    fun `initialSize 가 음수이면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(initialSize = -1)
        }
    }

    @Test
    fun `initialSize 가 maxSize 보다 크면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(maxSize = 10, initialSize = 11)
        }
    }

    @Test
    fun `minIdle 이 maxSize 보다 크면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(maxSize = 5, minIdle = 6)
        }
    }

    @Test
    fun `acquireRetry 가 음수이면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(acquireRetry = -1)
        }
    }

    @Test
    fun `maxPendingAcquire 가 -1 보다 작으면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(maxPendingAcquire = -2)
        }
    }

    @Test
    fun `validationQuery 가 공백이면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(validationQuery = "   ")
        }
    }

    @Test
    fun `poolName 이 공백이면 validate 에서 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(poolName = "  ")
        }
    }

    @Test
    fun `registerJmx 가 true 이면 poolName 이 필수이다`() {
        assertFailsWith<IllegalArgumentException> {
            R2dbcPoolConfig(registerJmx = true, poolName = null)
        }
    }

    @Test
    fun `validate - maxPendingAcquire 가 -1 이면 무제한으로 허용된다`() {
        val config = R2dbcPoolConfig(maxPendingAcquire = -1)
        config.maxPendingAcquire shouldBeEqualTo -1
    }

    // ─── highThroughput preset ──────────────────────────────────────

    @Test
    fun `highThroughput 프리셋 기본값 확인`() {
        val config = R2dbcPoolConfig.highThroughput()

        config.maxIdleTime shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_MAX_IDLE_TIME
        config.maxCreateConnectionTime shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_MAX_CREATE_CONNECTION_TIME
        config.acquireRetry shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_ACQUIRE_RETRY
        config.backgroundEvictionInterval shouldBeEqualTo R2dbcPoolConfig.HIGH_THROUGHPUT_BACKGROUND_EVICTION_INTERVAL
        config.validationQuery.shouldBeNull()
        config.registerJmx.shouldBeFalse()
    }

    @Test
    fun `highThroughput - warmupSize 는 maxSize 를 초과할 수 없다`() {
        val maxSize = 10
        val config = R2dbcPoolConfig.highThroughput(maxSize = maxSize, warmupSize = 100)

        config.initialSize shouldBeLessOrEqualTo maxSize
        config.minIdle shouldBeLessOrEqualTo maxSize
    }

    @Test
    fun `highThroughput - poolName 을 지정할 수 있다`() {
        val config = R2dbcPoolConfig.highThroughput(poolName = "my-pool")
        config.poolName shouldBeEqualTo "my-pool"
    }

    // ─── toConnectionPoolConfiguration ─────────────────────────────

    @Test
    fun `toConnectionPoolConfiguration - ConnectionPool 을 생성할 수 있다`() {
        val factory = h2Factory()
        val config = R2dbcPoolConfig(maxSize = 10, initialSize = 2, minIdle = 1)
        val poolConfig = config.toConnectionPoolConfiguration(factory)

        poolConfig.shouldNotBeNull()
    }

    @Test
    fun `connectionPoolOf factory - ConnectionPool 생성`() {
        val factory = h2Factory()
        val pool = connectionPoolOf(factory) {
            maxSize = 10
            initialSize = 2
            minIdle = 1
        }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()
        pool.close()
    }

    @Test
    fun `connectionPoolOf options - ConnectionPool 생성`() {
        val options = h2Options()
        val pool = connectionPoolOf(options) {
            maxSize = 20
            initialSize = 4
            minIdle = 2
        }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()
        pool.close()
    }

    @Test
    fun `toConnectionPool - ConnectionFactoryOptions 확장 함수`() {
        val pool = h2Options().toConnectionPool {
            maxSize = 20
            initialSize = 4
            minIdle = 2
        }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()
        pool.close()
    }

    @Test
    fun `connectionPoolOf - R2dbcPoolConfig 기본값으로 생성`() {
        val factory = h2Factory()
        val pool = connectionPoolOf(factory, R2dbcPoolConfig())

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.close()
    }

    @Test
    fun `toConnectionPool - R2dbcPoolConfig 기본값으로 생성`() {
        val pool = h2Options().toConnectionPool()

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.close()
    }

    // ─── maxPendingAcquire 경계값 ────────────────────────────────────

    @Test
    fun `maxPendingAcquire 양수 설정이 풀에 적용된다`() {
        val factory = h2Factory()
        val pool = connectionPoolOf(factory) {
            maxSize = 20
            initialSize = 2
            minIdle = 2
            maxPendingAcquire = 10
        }

        pool.shouldNotBeNull()
        pool.close()
    }

    // ─── validationQuery 설정 ────────────────────────────────────────

    @Test
    fun `validationQuery 설정 시 ConnectionPool 생성 성공`() {
        val factory = h2Factory()
        val pool = connectionPoolOf(factory) {
            maxSize = 20
            initialSize = 2
            minIdle = 2
            validationQuery = "SELECT 1"
        }

        pool.shouldNotBeNull()
        pool.close()
    }

    // ─── highThroughput + pool creation ──────────────────────────────

    @Test
    fun `highThroughput 프리셋으로 ConnectionPool 생성`() {
        val factory = h2Factory()
        val highConfig = R2dbcPoolConfig.highThroughput(maxSize = 20)
        val pool = connectionPoolOf(factory, highConfig)

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.close()
    }

    // ─── Duration defaults ────────────────────────────────────────────

    @Test
    fun `DEFAULT_MAX_IDLE_TIME 는 10분이다`() {
        R2dbcPoolConfig.DEFAULT_MAX_IDLE_TIME shouldBeEqualTo Duration.ofMinutes(10)
    }

    @Test
    fun `DEFAULT_MAX_LIFE_TIME 는 30분이다`() {
        R2dbcPoolConfig.DEFAULT_MAX_LIFE_TIME shouldBeEqualTo Duration.ofMinutes(30)
    }

    @Test
    fun `DEFAULT_MAX_CREATE_CONNECTION_TIME 는 10초이다`() {
        R2dbcPoolConfig.DEFAULT_MAX_CREATE_CONNECTION_TIME shouldBeEqualTo Duration.ofSeconds(10)
    }

    @Test
    fun `HIGH_THROUGHPUT_MAX_IDLE_TIME 은 5분이다`() {
        R2dbcPoolConfig.HIGH_THROUGHPUT_MAX_IDLE_TIME shouldBeEqualTo Duration.ofMinutes(5)
    }
}

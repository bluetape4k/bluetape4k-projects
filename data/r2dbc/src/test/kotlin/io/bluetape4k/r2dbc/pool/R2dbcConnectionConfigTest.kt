package io.bluetape4k.r2dbc.pool

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.junit.jupiter.api.Test
import java.time.Duration

class R2dbcConnectionConfigTest {

    companion object: KLoggingChannel()

    private fun h2R2dbcUrl(dbName: String = "url_test_${System.nanoTime()}") =
        "r2dbc:h2:mem:///$dbName;DB_CLOSE_DELAY=-1"

    @Test
    fun `connectionFactoryOptionsOf - H2 인메모리 설정`() {
        val options = connectionFactoryOptionsOf {
            driver = "h2"
            protocol = "mem"
            database = "test_${System.nanoTime()}"
            option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            lockWaitTimeout = Duration.ofSeconds(2)
        }
        log.debug { "options: $options" }

        options.shouldNotBeNull()
        options.getRequiredValue(ConnectionFactoryOptions.DRIVER) shouldBeEqualTo "h2"
        options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL) shouldBeEqualTo "mem"
        options.getRequiredValue(ConnectionFactoryOptions.LOCK_WAIT_TIMEOUT) shouldBeEqualTo Duration.ofSeconds(2)
    }

    @Test
    fun `connectionFactoryOptionsOf - MySQL 설정`() {
        val options = connectionFactoryOptionsOf {
            driver = "mysql"
            host = "localhost"
            port = 3306
            database = "mydb"
            user = "root"
            password = "secret"
            ssl = false
            connectTimeout = Duration.ofSeconds(10)
            option(Option.valueOf("useServerPrepareStatement"), true)
            option(Option.valueOf("tcpKeepAlive"), true)
        }
        log.debug { "options: $options" }

        options.getRequiredValue(ConnectionFactoryOptions.DRIVER) shouldBeEqualTo "mysql"
        options.getRequiredValue(ConnectionFactoryOptions.HOST) shouldBeEqualTo "localhost"
        options.getRequiredValue(ConnectionFactoryOptions.PORT) shouldBeEqualTo 3306
        options.getRequiredValue(ConnectionFactoryOptions.DATABASE) shouldBeEqualTo "mydb"
        options.getRequiredValue(ConnectionFactoryOptions.USER) shouldBeEqualTo "root"
        options.getRequiredValue(ConnectionFactoryOptions.SSL) shouldBeEqualTo false
    }

    @Test
    fun `connectionFactoryOptionsOf - PostgreSQL 설정`() {
        val options = connectionFactoryOptionsOf {
            driver = "postgresql"
            host = "localhost"
            port = 5432
            database = "mydb"
            user = "postgres"
            password = "secret"
            ssl = false
            connectTimeout = Duration.ofSeconds(10)
            statementTimeout = Duration.ofSeconds(30)
        }
        log.debug { "options: $options" }

        options.getRequiredValue(ConnectionFactoryOptions.DRIVER) shouldBeEqualTo "postgresql"
        options.getRequiredValue(ConnectionFactoryOptions.HOST) shouldBeEqualTo "localhost"
        options.getRequiredValue(ConnectionFactoryOptions.PORT) shouldBeEqualTo 5432
        options.getRequiredValue(ConnectionFactoryOptions.CONNECT_TIMEOUT) shouldBeEqualTo Duration.ofSeconds(10)
        options.getRequiredValue(ConnectionFactoryOptions.STATEMENT_TIMEOUT) shouldBeEqualTo Duration.ofSeconds(30)
    }

    @Test
    fun `connectionFactoryOptionsOf - driver 미설정 시 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            connectionFactoryOptionsOf {
                // driver 미설정
                host = "localhost"
            }
        }
    }

    @Test
    fun `connectionFactoryOptionsOf - ssl 기본값은 false`() {
        val options = connectionFactoryOptionsOf {
            driver = "h2"
            protocol = "mem"
            database = "test"
        }
        log.debug { "options: $options" }

        options.getRequiredValue(ConnectionFactoryOptions.SSL) shouldBeEqualTo false
    }

    @Test
    fun `connectionFactoryOptionsOf - 선택 필드 미설정 시 null`() {
        val options = connectionFactoryOptionsOf {
            driver = "h2"
            protocol = "mem"
            database = "test"
        }
        log.debug { "options: $options" }

        options.getValue(ConnectionFactoryOptions.HOST).shouldBeNull()
        options.getValue(ConnectionFactoryOptions.PORT).shouldBeNull()
        options.getValue(ConnectionFactoryOptions.USER).shouldBeNull()
        options.getValue(ConnectionFactoryOptions.CONNECT_TIMEOUT).shouldBeNull()
        options.getValue(ConnectionFactoryOptions.STATEMENT_TIMEOUT).shouldBeNull()
    }

    @Test
    fun `connectionFactoryOf - ConnectionFactory 생성`() {
        val factory = connectionFactoryOf {
            driver = "h2"
            protocol = "mem"
            database = "factory_test_${System.nanoTime()}"
            option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
        }

        factory.shouldNotBeNull()
        factory.shouldBeInstanceOf<ConnectionFactory>()
    }

    @Test
    fun `r2dbcConnectionPool - 연결과 풀을 한 번에 구성`() = runSuspendIO {
        val pool = r2dbcConnectionPool {
            connection {
                driver = "h2"
                protocol = "mem"
                database = "combined_test_${System.nanoTime()}"
                option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            }
            pool {
                maxSize = 20
                initialSize = 4
                minIdle = 2
                maxIdleTime = Duration.ofMinutes(5)
            }
        }
        log.debug { "pool: $pool" }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()

        log.debug { "통합 DSL ConnectionPool 생성 완료. isDisposed=${pool.isDisposed}" }
        pool.close().awaitSingleOrNull()
    }

    @Test
    fun `r2dbcConnectionPool - pool 블록 생략 시 기본 풀 설정 사용`() = runSuspendIO {
        val pool = r2dbcConnectionPool {
            connection {
                driver = "h2"
                protocol = "mem"
                database = "default_pool_test_${System.nanoTime()}"
                option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            }
            // pool 블록 생략 → R2dbcPoolConfig 기본값 사용
        }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.close().awaitSingleOrNull()
    }

    // ─── URL 기반 API ───────────────────────────────────────────────

    @Test
    fun `connectionFactoryOptionsOf(url) - H2 URL 파싱`() {
        val options = connectionFactoryOptionsOf(h2R2dbcUrl())

        log.debug { "URL 파싱 ConnectionFactoryOptions: $options" }
        options.shouldNotBeNull()
        options.getRequiredValue(ConnectionFactoryOptions.DRIVER) shouldBeEqualTo "h2"
    }

    @Test
    fun `connectionFactoryOf(url) - H2 URL로 ConnectionFactory 생성`() {
        val factory = connectionFactoryOf(h2R2dbcUrl())

        factory.shouldNotBeNull()
        factory.shouldBeInstanceOf<ConnectionFactory>()
    }

    @Test
    fun `r2dbcConnectionPool(url) - URL로 ConnectionPool 생성`() = runSuspendIO {
        val pool = r2dbcConnectionPool(h2R2dbcUrl())

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()
        pool.close().awaitSingleOrNull()
    }

    @Test
    fun `r2dbcConnectionPool(url) - URL + 풀 설정 람다`() = runSuspendIO {
        val pool = r2dbcConnectionPool(h2R2dbcUrl()) {
            maxSize = 20
            initialSize = 4
            minIdle = 2
            maxIdleTime = Duration.ofMinutes(3)
        }

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()

        log.debug { "URL 기반 ConnectionPool 생성 완료. isDisposed=${pool.isDisposed}" }
        pool.close().awaitSingleOrNull()
    }

    @Test
    fun `password 타입이 String 으로 할당 가능`() {
        val options = connectionFactoryOptionsOf {
            driver = "h2"
            protocol = "mem"
            database = "pw_test"
            user = "sa"
            password = "mySecret123"   // String 직접 할당
        }
        log.debug { "options: $options" }

        options.getRequiredValue(ConnectionFactoryOptions.USER) shouldBeEqualTo "sa"
        // password는 sensitive option이라 CharSequence로 저장됨
        options.getValue(ConnectionFactoryOptions.PASSWORD).shouldNotBeNull()
    }
}

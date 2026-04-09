package io.bluetape4k.exposed.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

private fun h2R2dbcUrl(dbName: String = "url_test_${System.nanoTime()}") =
    "r2dbc:h2:mem:///$dbName;DB_CLOSE_DELAY=-1"

class R2dbcConnectionConfigTest {

    companion object : KLogging()

    @Test
    fun `connectionFactoryOptionsOf - H2 인메모리 설정`() {
        val options = connectionFactoryOptionsOf {
            driver = "h2"
            protocol = "mem"
            database = "test_${System.nanoTime()}"
            option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
        }

        options.shouldNotBeNull()
        options.getRequiredValue(ConnectionFactoryOptions.DRIVER) shouldBeEqualTo "h2"
        options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL) shouldBeEqualTo "mem"

        log.debug { "H2 ConnectionFactoryOptions: $options" }
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

        options.getRequiredValue(ConnectionFactoryOptions.SSL) shouldBeEqualTo false
    }

    @Test
    fun `connectionFactoryOptionsOf - 선택 필드 미설정 시 null`() {
        val options = connectionFactoryOptionsOf {
            driver = "h2"
            protocol = "mem"
            database = "test"
        }

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
    fun `r2dbcConnectionPool - 연결과 풀을 한 번에 구성`() {
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

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()

        log.debug { "통합 DSL ConnectionPool 생성 완료. isDisposed=${pool.isDisposed}" }
        pool.close()
    }

    @Test
    fun `r2dbcConnectionPool - pool 블록 생략 시 기본 풀 설정 사용`() {
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
        pool.close()
    }

    // ─── URL 기반 API ───────────────────────────────────────────────

    @Test
    fun `connectionFactoryOptionsOf(url) - H2 URL 파싱`() {
        val options = connectionFactoryOptionsOf(h2R2dbcUrl())

        options.shouldNotBeNull()
        options.getRequiredValue(ConnectionFactoryOptions.DRIVER) shouldBeEqualTo "h2"

        log.debug { "URL 파싱 ConnectionFactoryOptions: $options" }
    }

    @Test
    fun `connectionFactoryOf(url) - H2 URL로 ConnectionFactory 생성`() {
        val factory = connectionFactoryOf(h2R2dbcUrl())

        factory.shouldNotBeNull()
        factory.shouldBeInstanceOf<ConnectionFactory>()
    }

    @Test
    fun `r2dbcConnectionPool(url) - URL로 ConnectionPool 생성`() {
        val pool = r2dbcConnectionPool(h2R2dbcUrl())

        pool.shouldNotBeNull()
        pool.shouldBeInstanceOf<ConnectionPool>()
        pool.isDisposed.shouldBeFalse()
        pool.close()
    }

    @Test
    fun `r2dbcConnectionPool(url) - URL + 풀 설정 람다`() {
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
        pool.close()
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

        options.getRequiredValue(ConnectionFactoryOptions.USER) shouldBeEqualTo "sa"
        // password는 sensitive option이라 CharSequence로 저장됨
        options.getValue(ConnectionFactoryOptions.PASSWORD).shouldNotBeNull()
    }
}

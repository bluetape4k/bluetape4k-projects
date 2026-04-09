package io.bluetape4k.jdbc.hikari

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import javax.sql.DataSource

private fun h2JdbcUrl(dbName: String = "hikari_test_${System.nanoTime()}") =
    "jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1"

class HikariSupportTest {

    companion object : KLogging()

    @Test
    fun `hikariConfigOf - DSL 람다로 HikariConfig 생성`() {
        val config = hikariConfigOf {
            jdbcUrl = h2JdbcUrl()
            username = "sa"
            password = ""
            maximumPoolSize = 10
            minimumIdle = 2
            poolName = "testPool"
        }

        config.shouldNotBeNull()
        config.maximumPoolSize shouldBeEqualTo 10

        log.debug { "HikariConfig: jdbcUrl=${config.jdbcUrl}, poolName=${config.poolName}" }
    }

    @Test
    fun `hikariDataSourceOf - DSL 람다로 HikariDataSource 생성`() {
        val ds = hikariDataSourceOf {
            jdbcUrl = h2JdbcUrl()
            username = "sa"
            password = ""
            maximumPoolSize = 5
            minimumIdle = 1
        }

        ds.shouldNotBeNull()
        ds.shouldBeInstanceOf<DataSource>()
        ds.isClosed.shouldBeFalse()

        log.debug { "HikariDataSource: isClosed=${ds.isClosed}" }
        ds.close()
    }

    @Test
    fun `hikariDataSourceOf - URL 전용으로 HikariDataSource 생성`() {
        val ds = hikariDataSourceOf(h2JdbcUrl())

        ds.shouldNotBeNull()
        ds.isClosed.shouldBeFalse()
        ds.close()
    }

    @Test
    fun `hikariDataSourceOf - URL + 인증 정보로 HikariDataSource 생성`() {
        val ds = hikariDataSourceOf(
            jdbcUrl = h2JdbcUrl(),
            username = "sa",
            password = "",
        )

        ds.shouldNotBeNull()
        ds.isClosed.shouldBeFalse()
        ds.close()
    }

    @Test
    fun `hikariDataSourceOf - URL + 인증 정보 + 추가 설정으로 HikariDataSource 생성`() {
        val ds = hikariDataSourceOf(
            jdbcUrl = h2JdbcUrl(),
            username = "sa",
            password = "",
        ) {
            maximumPoolSize = 20
            minimumIdle = 4
            connectionTimeout = 30_000
            idleTimeout = 600_000
            poolName = "customPool"
        }

        ds.shouldNotBeNull()
        ds.isClosed.shouldBeFalse()

        log.debug { "HikariDataSource (URL+DSL): isClosed=${ds.isClosed}" }
        ds.close()
    }

    @Test
    fun `hikariDataSourceOf - 연결 후 쿼리 실행`() {
        hikariDataSourceOf(h2JdbcUrl("query_test_${System.nanoTime()}")).use { ds ->
            ds.shouldNotBeNull()
            ds.connection.use { conn ->
                conn.shouldNotBeNull()
                val rs = conn.createStatement().executeQuery("SELECT 1")
                rs.next()
                val result = rs.getInt(1)
                result shouldBeEqualTo 1
                log.debug { "쿼리 결과: $result" }
            }
        }
    }
}

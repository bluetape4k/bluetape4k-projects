package io.bluetape4k.exposed.clickhouse

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

/**
 * ClickHouse JDBC 0.9.5 트랜잭션 동작 PoC 테스트.
 *
 * PoC 목적:
 * 1. raw Connection.commit()/rollback()이 throw하는지 no-op인지 확인
 * 2. requiresAutoCommitOnCreateDrop flag의 DDL 영향 확인
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClickHouseTransactionPocTest : AbstractClickHouseTest() {

    companion object : KLogging()

    private fun rawConnection(): Connection {
        val props = Properties().apply {
            setProperty("user", clickhouse.username ?: "test")
            setProperty("password", clickhouse.password ?: "test")
        }
        return DriverManager.getConnection(clickhouse.jdbcUrl, props)
    }

    /**
     * C1 검증: autoCommit=true 상태에서 raw commit()이 예외 없이 반환되는지 확인.
     * ClickHouseConnectionWrapper의 no-op commit() 구현이 타당함을 실 컨테이너로 검증.
     */
    @Test
    fun `raw commit does not throw exception`() {
        rawConnection().use { conn ->
            conn.autoCommit = true
            conn.commit() // 예외 없어야 함 (no-op)
            log.info("raw commit() with autoCommit=true: OK (no-op)")
        }
    }

    /**
     * C1 검증: autoCommit=true 상태에서 raw rollback()이 예외 없이 반환되는지 확인.
     */
    @Test
    fun `raw rollback does not throw exception`() {
        rawConnection().use { conn ->
            conn.autoCommit = true
            conn.rollback() // 예외 없어야 함 (no-op)
            log.info("raw rollback() with autoCommit=true: OK (no-op)")
        }
    }

    /**
     * C2 검증: ClickHouseConnectionWrapper가 autoCommit을 true로 강제하는지 확인.
     * requiresAutoCommitOnCreateDrop=true와 함께 DDL 실행에 문제가 없음을 보장.
     */
    @Test
    fun `autoCommit is always true in wrapper`() {
        transaction(db) {
            connection.autoCommit.shouldBeTrue()
        }
    }

    /**
     * SELECT 1이 트랜잭션 내에서 정상 동작하는지 확인.
     */
    @Test
    fun `SELECT 1 works in transaction`() {
        val result = transaction(db) {
            exec("SELECT 1") { rs -> rs.next(); rs.getInt(1) }
        }
        result shouldBeEqualTo 1
    }
}

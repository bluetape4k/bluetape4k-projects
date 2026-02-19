package io.bluetape4k.jdbc.sql

import io.bluetape4k.jdbc.model.Actor
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * DataSourceTransactionExtensions 테스트 클래스
 *
 * DataSource 트랜잭션 관련 확장 함수들의 테스트를 제공합니다.
 */
class DataSourceTransactionExtensionsTest : AbstractJdbcSqlTest() {
    @Test
    fun `DataSource withTransaction - 트랜잭션 실행`() {
        val result =
            dataSource.withTransaction { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('DS', 'Transaction')")
                }
                42
            }

        result shouldBeEqualTo 42

        // 커밋 확인
        val actor =
            dataSource.runQuery(
                "SELECT * FROM Actors WHERE firstname = 'DS' AND lastname = 'Transaction'",
            ) { rs ->
                if (rs.next()) {
                    Actor(
                        id = rs.getInt("id"),
                        firstname = rs.getString("firstname"),
                        lastname = rs.getString("lastname"),
                    )
                } else {
                    null
                }
            }
        actor.shouldNotBeNull()
    }

    @Test
    fun `DataSource withTransaction - 롤백 확인`() {
        assertThrows<RuntimeException> {
            dataSource.withTransaction { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('DS', 'Rollback')")
                }
                throw RuntimeException("의도적인 예외")
            }
        }

        // 롤백 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE firstname = 'DS' AND lastname = 'Rollback'",
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 0
    }
}

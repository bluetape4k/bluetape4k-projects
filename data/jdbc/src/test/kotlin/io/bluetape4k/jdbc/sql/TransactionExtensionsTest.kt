package io.bluetape4k.jdbc.sql

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection

/**
 * TransactionExtensions 테스트 클래스
 *
 * 데이터베이스 트랜잭션 관련 확장 함수들의 테스트를 제공합니다.
 */
class TransactionExtensionsTest : AbstractJdbcSqlTest() {
    @Test
    fun `withTransaction - 트랜잭션 내에서 작업 수행 후 자동 커밋`() {
        dataSource.withTransaction { conn ->
            conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Transaction', 'Test')")
        }

        // 데이터가 커밋되었는지 확인
        val count =
            dataSource.runQuery("SELECT COUNT(*) FROM Actors WHERE firstname = 'Transaction'") { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 1
    }

    @Test
    fun `withTransaction - 예외 발생 시 자동 롤백`() {
        assertThrows<RuntimeException> {
            dataSource.withTransaction { conn ->
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Rollback', 'Test')")
                throw RuntimeException("의도적인 예외")
            }
        }

        // 데이터가 롤백되었는지 확인
        val count =
            dataSource.runQuery("SELECT COUNT(*) FROM Actors WHERE firstname = 'Rollback'") { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 0
    }

    @Test
    fun `withTransaction - 결과값 반환`() {
        val result =
            dataSource.withTransaction { conn ->
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Result', 'Test')")
                42 // 결과 반환
            }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withReadOnlyTransaction - 읽기 전용 트랜잭션`() {
        dataSource.withReadOnlyTransaction { conn ->
            val count =
                conn.runQuery("SELECT COUNT(*) FROM Actors") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            count shouldBeGreaterThan 0
        }
    }

    @Test
    fun `withTransaction - 격리 수준 지정`() {
        dataSource.withTransaction(Connection.TRANSACTION_SERIALIZABLE) { conn ->
            val level = conn.transactionIsolation
            level shouldBeEqualTo Connection.TRANSACTION_SERIALIZABLE
        }
    }

    @Test
    fun `Connection withAutoCommit - auto-commit 상태 임시 변경`() {
        dataSource.withConnect { conn ->
            conn.withAutoCommit(false) { connection ->
                connection.autoCommit.shouldBeFalse()
            }
            conn.autoCommit.shouldBeTrue() // 원래 상태로 복원
        }
    }

    @Test
    fun `Connection withReadOnly - 읽기 전용 모드 임시 변경`() {
        dataSource.withConnect { conn ->
            val originalReadOnly = conn.isReadOnly

            conn.withReadOnly { connection ->
                // H2에서는 isReadOnly 설정이 지원되지 않을 수 있음
                // 설정 시도 자체가 예외를 발생시키지 않는지 확인
            }

            // 원래 상태로 복원되었는지 확인
            conn.isReadOnly shouldBeEqualTo originalReadOnly
        }
    }

    @Test
    fun `Connection withIsolationLevel - 격리 수준 임시 변경`() {
        dataSource.withConnect { conn ->
            val originalLevel = conn.transactionIsolation

            conn.withIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED) { connection ->
                connection.transactionIsolation shouldBeEqualTo Connection.TRANSACTION_READ_UNCOMMITTED
            }

            conn.transactionIsolation shouldBeEqualTo originalLevel // 원래 상태로 복원
        }
    }

    @Test
    fun `복합 트랜잭션 작업`() {
        val actorId =
            dataSource.withTransaction { conn ->
                // 첫 번째 INSERT
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Actor', 'One')")

                // 두 번째 INSERT
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Actor', 'Two')")

                // 결과 확인
                conn.runQuery("SELECT id FROM Actors WHERE firstname = 'Actor' AND lastname = 'One'") { rs ->
                    rs.next()
                    rs.getInt("id")
                }
            }

        actorId.shouldNotBeNull()
        actorId shouldBeGreaterThan 0

        // 두 개의 레코드가 모두 커밋되었는지 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE firstname = 'Actor' AND lastname IN ('One', 'Two')",
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 2
    }

    @Test
    fun `트랜잭션 중간 예외 - 롤백 확인`() {
        try {
            dataSource.withTransaction { conn ->
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Before', 'Exception')")

                // 의도적인 예외 발생
                error("의도적인 에러")
            }
        } catch (e: IllegalStateException) {
            // 예외 발생 확인
        }

        // 롤백되었는지 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE firstname = 'Before'",
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 0
    }
}

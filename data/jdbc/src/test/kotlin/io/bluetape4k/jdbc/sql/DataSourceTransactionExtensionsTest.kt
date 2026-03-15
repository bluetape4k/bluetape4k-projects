package io.bluetape4k.jdbc.sql

import io.bluetape4k.jdbc.model.Actor
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * DataSourceTransactionExtensions 테스트 클래스
 *
 * DataSource 트랜잭션 및 파라미터 바인딩 관련 확장 함수들의 테스트를 제공합니다.
 */
class DataSourceTransactionExtensionsTest: AbstractJdbcSqlTest() {
    // ─── withTransaction ──────────────────────────────────────────────────────

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
                "SELECT * FROM Actors WHERE firstname = 'DS' AND lastname = 'Transaction'"
            ) { rs ->
                if (rs.next()) {
                    Actor(
                        id = rs.getInt("id"),
                        firstname = rs.getString("firstname"),
                        lastname = rs.getString("lastname")
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
                "SELECT COUNT(*) FROM Actors WHERE firstname = 'DS' AND lastname = 'Rollback'"
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 0
    }

    // ─── withReadOnlyTransaction ───────────────────────────────────────────────

    @Test
    fun `DataSource withReadOnlyTransaction - 읽기 전용 트랜잭션으로 조회`() {
        val actors =
            dataSource.withReadOnlyTransaction { conn ->
                conn.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                    rs.toList { row ->
                        Actor(
                            id = row.getInt("id"),
                            firstname = row.getString("firstname"),
                            lastname = row.getString("lastname")
                        )
                    }
                }
            }

        actors.shouldNotBeEmpty()
        actors.first().firstname shouldBeEqualTo "Sunghyouk"
    }

    // ─── executeQuery with params ─────────────────────────────────────────────

    @Test
    fun `DataSource executeQuery - 파라미터 바인딩 조회`() {
        val actor =
            dataSource.executeQuery(
                "SELECT * FROM Actors WHERE id = ?",
                1
            ) { rs ->
                if (rs.next()) {
                    Actor(
                        id = rs.getInt("id"),
                        firstname = rs.getString("firstname"),
                        lastname = rs.getString("lastname")
                    )
                } else {
                    null
                }
            }

        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo 1
        actor.firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `DataSource executeQuery - 결과 없는 파라미터 조회는 null 반환`() {
        val actor =
            dataSource.executeQuery(
                "SELECT * FROM Actors WHERE id = ?",
                -999
            ) { rs ->
                if (rs.next()) {
                    Actor(rs.getInt("id"), rs.getString("firstname"), rs.getString("lastname"))
                } else {
                    null
                }
            }

        actor shouldBeEqualTo null
    }

    // ─── executeUpdate with params ────────────────────────────────────────────

    @Test
    fun `DataSource executeUpdate - 파라미터 바인딩 업데이트`() {
        // 초기 값 삽입
        dataSource.withTransaction { conn ->
            conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Param', 'Update')")
        }

        val affected =
            dataSource.executeUpdate(
                "UPDATE Actors SET lastname = ? WHERE firstname = ?",
                "UpdatedParam",
                "Param"
            )

        affected shouldBeGreaterThan 0

        // 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE firstname = 'Param' AND lastname = 'UpdatedParam'"
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 1
    }

    // ─── executeUpdateWithGeneratedKeys ───────────────────────────────────────

    @Test
    fun `DataSource executeUpdateWithGeneratedKeys - 생성된 키 반환`() {
        val generatedKey =
            dataSource.executeUpdateWithGeneratedKeys(
                "INSERT INTO Actors (firstname, lastname) VALUES (?, ?)",
                "Generated",
                "Key"
            ) { rs ->
                if (rs.next()) rs.getLong(1) else null
            }

        generatedKey.shouldNotBeNull()
        generatedKey shouldBeGreaterThan 0L
    }

    // ─── executeBatch ─────────────────────────────────────────────────────────

    @Test
    fun `DataSource executeBatch - 배치 삽입`() {
        val paramsList =
            listOf(
                listOf("Batch1", "Actor"),
                listOf("Batch2", "Actor"),
                listOf("Batch3", "Actor")
            )

        val results =
            dataSource.executeBatch(
                "INSERT INTO Actors (firstname, lastname) VALUES (?, ?)",
                paramsList
            )

        results.shouldNotBeEmpty()

        // 모든 행이 삽입되었는지 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE lastname = 'Actor'"
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 3
    }

    @Test
    fun `DataSource executeBatch - 빈 paramsList 는 빈 결과 반환`() {
        val results =
            dataSource.executeBatch(
                "INSERT INTO Actors (firstname, lastname) VALUES (?, ?)",
                emptyList()
            )

        results shouldBeEqualTo emptyList()
    }

    // ─── executeLargeBatch ────────────────────────────────────────────────────

    @Test
    fun `DataSource executeLargeBatch - 대량 배치 삽입`() {
        val paramsList = (1..5).map { i -> listOf("LargeBatch$i", "LargeActor") }

        val results =
            dataSource.executeLargeBatch(
                "INSERT INTO Actors (firstname, lastname) VALUES (?, ?)",
                paramsList
            )

        results.size shouldBeEqualTo 5
        results.all { it >= 0L }.shouldBeTrue()

        // 삽입 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE lastname = 'LargeActor'"
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 5
    }
}

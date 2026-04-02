package io.bluetape4k.exposed.trino

import io.bluetape4k.exposed.trino.dialect.TrinoDialect
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

/**
 * [TrinoDatabase] 연결 팩토리 및 [TrinoDialect] 기본 동작을 검증하는 테스트.
 */
class TrinoDatabaseTest : AbstractTrinoTest() {

    @Test
    fun `db dialect 는 TrinoDialect 인스턴스이다`() {
        db.dialect.shouldBeInstanceOf<TrinoDialect>()
    }

    @Test
    fun `db dialect name 은 trino 이다`() {
        db.dialect.name shouldBeEqualTo "trino"
    }

    @Test
    fun `transaction exec SELECT 1 은 정상 실행된다`() {
        transaction(db) {
            exec("SELECT 1") { rs ->
                rs.next()
                rs.getInt(1)
            }.shouldNotBeNull()
        }
    }

    @Test
    fun `connect host port catalog schema 방식으로 연결 성공`() {
        val newDb = TrinoDatabase.connect(
            host = trino.host,
            port = trino.port,
            catalog = "memory",
            schema = "default",
            user = trino.username ?: "trino",
        )
        newDb.shouldNotBeNull()
        transaction(newDb) {
            exec("SELECT 1") { rs ->
                rs.next()
                rs.getInt(1)
            }.shouldNotBeNull()
        }
    }

    @Test
    fun `connect jdbcUrl 방식으로 연결 성공`() {
        val jdbcUrl = "jdbc:trino://${trino.host}:${trino.port}/memory/default"
        val newDb = TrinoDatabase.connect(
            jdbcUrl = jdbcUrl,
            user = trino.username ?: "trino",
        )
        newDb.shouldNotBeNull()
        transaction(newDb) {
            exec("SELECT 1") { rs ->
                rs.next()
                rs.getInt(1)
            }.shouldNotBeNull()
        }
    }

    @Test
    fun `supportsColumnTypeChange 는 false 이다`() {
        db.dialect.supportsColumnTypeChange shouldBeEqualTo false
    }

    @Test
    fun `supportsMultipleGeneratedKeys 는 false 이다`() {
        db.dialect.supportsMultipleGeneratedKeys shouldBeEqualTo false
    }
}

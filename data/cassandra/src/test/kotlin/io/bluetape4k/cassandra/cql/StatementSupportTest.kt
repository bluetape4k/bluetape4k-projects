package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.BatchType
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cassandra.toCqlIdentifier
import org.junit.jupiter.api.Test

class StatementSupportTest {

    @Test
    fun `statementOf 는 cql 문자열로 SimpleStatement 를 생성한다`() {
        val statement = statementOf("SELECT now() FROM system.local")
        statement.query shouldBeEqualTo "SELECT now() FROM system.local"
    }

    @Test
    fun `statementOf 는 위치 및 이름 기반 파라미터를 보존한다`() {
        statementOf("SELECT * FROM ks.tbl WHERE id=?", 7).positionalValues.toList() shouldBeEqualTo listOf(7)
        statementOf("SELECT * FROM ks.tbl WHERE id=:id", mapOf("id" to 7)).namedValues shouldBeEqualTo mapOf("id".toCqlIdentifier() to 7)
    }

    @Test
    fun `simpleStatementOf 는 builder 설정을 반영한다`() {
        val statement = simpleStatementOf("SELECT now() FROM system.local") {
            setPageSize(128)
        }
        statement.pageSize shouldBeEqualTo 128
    }

    @Test
    fun `batchStatementOf 는 builder 설정을 반영한다`() {
        val statement = statementOf("INSERT INTO ks.tbl (id) VALUES (1)")
        val batch = batchStatementOf(BatchType.LOGGED) {
            addStatement(statement)
        }

        batch.size() shouldBeEqualTo 1
    }

    @Suppress("DEPRECATION")
    @Test
    fun `batchStatementOf 는 vararg iterable template overload를 지원한다`() {
        val first = statementOf("INSERT INTO ks.tbl (id) VALUES (1)")
        val second = statementOf("INSERT INTO ks.tbl (id) VALUES (2)")

        batchStatementOf(BatchType.UNLOGGED, first, second).size() shouldBeEqualTo 2
        batchStatementOf(BatchType.UNLOGGED, listOf(first, second)).size() shouldBeEqualTo 2

        val template = batchStatementOf(BatchType.LOGGED, first)
        batchStatementOf(template) { addStatement(second) }.size() shouldBeEqualTo 2
        batchStatement(template) { addStatement(second) }.size() shouldBeEqualTo 2
    }

    @Test
    fun `batchStatementOf 는 빈 배치를 생성한다`() {
        batchStatementOf(BatchType.LOGGED).size() shouldBeEqualTo 0
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated statement 함수는 호환 동작한다`() {
        val statement = simpleStatement("SELECT now() FROM system.local") {
            setPageSize(64)
        }
        statement.pageSize shouldBeEqualTo 64

        val batch = batchStatement(BatchType.LOGGED) {
            addStatement(statement)
        }
        batch.size() shouldBeEqualTo 1
    }

    @Test
    fun `toPrepareRequest 는 PrepareRequest 를 생성한다`() {
        val request = statementOf("SELECT now() FROM system.local").toPrepareRequest()
        request.shouldNotBeNull()
    }

    @Test
    fun `blank query 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            statementOf(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            simpleStatementOf(" ") { }
        }
    }
}

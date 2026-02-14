package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.BatchType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class StatementSupportTest {

    @Test
    fun `statementOf 는 cql 문자열로 SimpleStatement 를 생성한다`() {
        val statement = statementOf("SELECT now() FROM system.local")
        statement.query shouldBeEqualTo "SELECT now() FROM system.local"
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

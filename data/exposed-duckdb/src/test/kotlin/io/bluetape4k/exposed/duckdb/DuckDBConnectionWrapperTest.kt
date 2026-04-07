package io.bluetape4k.exposed.duckdb

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.util.concurrent.atomic.AtomicReference

class DuckDBConnectionWrapperTest {

    @Test
    fun `generated key 관련 prepareStatement 오버로드는 기본 prepareStatement 로 위임한다`() {
        val lastSql = AtomicReference<String>()
        val statement = proxyPreparedStatement()
        val connection = proxyConnection(lastSql, statement)
        val wrapper = DuckDBConnectionWrapper(connection)

        (statement === wrapper.prepareStatement("select 1", Statement.RETURN_GENERATED_KEYS)) shouldBeEqualTo true
        lastSql.get() shouldBeEqualTo "select 1"

        (statement === wrapper.prepareStatement("select 2", intArrayOf(1))) shouldBeEqualTo true
        lastSql.get() shouldBeEqualTo "select 2"

        (statement === wrapper.prepareStatement("select 3", arrayOf("id"))) shouldBeEqualTo true
        lastSql.get() shouldBeEqualTo "select 3"
    }

    private fun proxyConnection(
        lastSql: AtomicReference<String>,
        statement: PreparedStatement,
    ): Connection = Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(Connection::class.java),
    ) { _, method, args ->
        when (method.name) {
            "prepareStatement" -> {
                lastSql.set(args[0] as String)
                statement
            }
            "isClosed"     -> false
            "close"        -> Unit
            "unwrap"       -> null
            "isWrapperFor" -> false
            "toString"     -> "proxyConnection"
            else           -> throw UnsupportedOperationException("Unexpected method: ${method.name}")
        }
    } as Connection

    private fun proxyPreparedStatement(): PreparedStatement = Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(PreparedStatement::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "close"    -> Unit
            "isClosed" -> false
            "unwrap"   -> null
            "isWrapperFor" -> false
            "toString" -> "proxyPreparedStatement"
            else       -> throw UnsupportedOperationException("Unexpected method: ${method.name}")
        }
    } as PreparedStatement
}

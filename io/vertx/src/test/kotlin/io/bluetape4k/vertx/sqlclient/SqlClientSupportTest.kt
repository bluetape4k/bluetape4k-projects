package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendRollback
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.junit.jupiter.api.Test

class SqlClientSupportTest: AbstractVertxSqlClientTest() {

    override val schemaFileNames: List<String> = listOf("person.sql")

    override fun Vertx.getPool() = getH2Pool()

    @Test
    fun `suspendQuery overloads execute plain and parameterized SQL`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
            val rows = conn.suspendQuery("select id, first_name from Person order by id")
            rows.size() shouldBeGreaterThan 0

            val names = conn.suspendQuery("select first_name from Person order by id") { row ->
                row.getString("first_name")
            }
            names shouldHaveSize rows.size()

            val parameterizedRows = conn.suspendQuery(
                "select id, first_name from Person where id = ?",
                Tuple.of(1),
            )
            parameterizedRows.size() shouldBeGreaterThan 0

            val parameterizedNames = conn.suspendQuery(
                "select first_name from Person where id = ?",
                Tuple.of(1),
            ) { row -> row.getString("first_name") }
            parameterizedNames shouldHaveSize parameterizedRows.size()
        }
    }
}

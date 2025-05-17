package io.bluetape4k.vertx.sqlclient.mybatis

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.sqlclient.schema.PersonMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.person
import io.bluetape4k.vertx.sqlclient.tests.testWithRollbackSuspending
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.SqlResult
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.mybatis.dynamic.sql.util.kotlin.elements.add
import org.mybatis.dynamic.sql.util.kotlin.elements.constant
import org.mybatis.dynamic.sql.util.kotlin.elements.max
import org.mybatis.dynamic.sql.util.kotlin.model.update

class MysqlSqlClientExtensionsTest: AbstractSqlClientExtensionsTest() {

    companion object: KLoggingChannel()

    override

    fun Vertx.getPool() = getMySQLPool()

    /**
     * Update set to subquery in mysql
     *
     * [MySQL Error 1093 : You can't specify target table 'tablename' for update in FROM clause](https://www.lesstif.com/dbms/mysql-error-1093-you-can-t-specify-target-table-tablename-for-update-in-from-clause-18220088.html)
     */
    @Test
    fun `update set to subquery in mysql`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val rows = conn.select(add(max(person.addressId), constant<Int>("1"))) {
                from(person)
                groupBy(person.addressId)
                orderBy(person.addressId.descending())
            }
            val maxAddressId = rows.firstOrNull()?.getInteger(0)
            log.debug { "maxAddressId=$maxAddressId" }

            val updateProvider = update(person) {
                set(person.addressId) equalToWhenPresent maxAddressId
                where { person.id isEqualTo 5 }
            }.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

            updateProvider.updateStatement shouldBeEqualTo
                    "update Person set address_id = #{p1} where id = #{p2}"

            updateProvider.parameters shouldContainSame mapOf("p1" to 3, "p2" to 5)

            val result: SqlResult<Void> = conn.update(updateProvider)
            result.rowCount() shouldBeEqualTo 1

            val person = conn.selectOne(listOf(person.allColumns()), PersonMapper) {
                from(person)
                where { person.id isEqualTo 5 }
            }
            person.shouldNotBeNull()
            person.addressId shouldBeEqualTo 3
        }
    }
}

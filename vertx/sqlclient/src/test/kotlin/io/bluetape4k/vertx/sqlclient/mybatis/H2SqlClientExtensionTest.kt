package io.bluetape4k.vertx.sqlclient.mybatis

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.vertx.sqlclient.schema.PersonMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.person
import io.bluetape4k.vertx.sqlclient.tests.testWithRollbackSuspending
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlConnection
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.mybatis.dynamic.sql.util.kotlin.elements.add
import org.mybatis.dynamic.sql.util.kotlin.elements.constant
import org.mybatis.dynamic.sql.util.kotlin.elements.max
import org.mybatis.dynamic.sql.util.kotlin.model.update

class H2SqlClientExtensionTest: AbstractSqlClientExtensionsTest() {

    companion object: KLoggingChannel()

    override fun Vertx.getPool() = this.getH2Pool()

    @Test
    fun `update set to subquery`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val updateProvider = update(person) {
                set(person.addressId) equalToQueryResult {
                    select(add(max(person.addressId), constant<Int>("1"))) { from(person) }
                }
                where { person.id isEqualTo 3 }
            }.renderForVertx()

            updateProvider.updateStatement shouldBeEqualTo
                    "update Person " +
                    "set address_id = (select (max(address_id) + 1) from Person) " +
                    "where id = #{p1}"

            updateProvider.parameters shouldContainSame mapOf("p1" to 3)

            val result = conn.update(updateProvider)
            result.rowCount() shouldBeEqualTo 1

            val person = conn.selectOne(listOf(person.allColumns()), PersonMapper) {
                from(person)
                where { person.id isEqualTo 3 }
            }
            person.shouldNotBeNull()
            person.addressId shouldBeEqualTo 3
        }
    }
}

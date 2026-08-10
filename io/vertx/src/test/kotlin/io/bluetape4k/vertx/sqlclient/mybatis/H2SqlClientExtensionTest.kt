package io.bluetape4k.vertx.sqlclient.mybatis

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.vertx.sqlclient.schema.PersonMapper
import io.bluetape4k.vertx.sqlclient.schema.Person
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.person
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendRollback
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlConnection
import org.junit.jupiter.api.Test
import org.mybatis.dynamic.sql.util.kotlin.elements.add
import org.mybatis.dynamic.sql.util.kotlin.elements.constant
import org.mybatis.dynamic.sql.util.kotlin.elements.max
import org.mybatis.dynamic.sql.util.kotlin.model.countDistinct
import org.mybatis.dynamic.sql.util.kotlin.model.countFrom
import org.mybatis.dynamic.sql.util.kotlin.model.deleteFrom
import org.mybatis.dynamic.sql.util.kotlin.model.insert
import org.mybatis.dynamic.sql.util.kotlin.model.insertMultiple
import org.mybatis.dynamic.sql.util.kotlin.model.insertInto
import org.mybatis.dynamic.sql.util.kotlin.model.select
import org.mybatis.dynamic.sql.util.kotlin.model.selectDistinct
import org.mybatis.dynamic.sql.util.kotlin.model.update

class H2SqlClientExtensionTest: AbstractSqlClientExtensionsTest() {

    companion object: KLoggingChannel()

    override fun Vertx.getPool() = this.getH2Pool()

    @Test
    fun `typed and convenience select overloads execute against H2`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
            val typedColumns = listOf(person.id, person.firstName alias "name")
            val provider = select(typedColumns) { from(person) }.renderForVertx()

            conn.selectAs<NameRecord>(provider).size() shouldBeGreaterThan 0
            conn.selectAs<NameRecord>(typedColumns) {
                from(person)
            }.size() shouldBeGreaterThan 0
            conn.selectListAs<NameRecord>(provider).size shouldBeGreaterThan 0
            conn.selectOneAs<NameRecord>(provider).shouldNotBeNull()

            conn.selectDistinct(person.lastName) { from(person) }.size() shouldBeGreaterThan 0
            conn.selectDistinct(listOf(person.lastName)) { from(person) }.size() shouldBeGreaterThan 0

            conn.countFrom(person) { allRows() } shouldBeGreaterThan 0L
            conn.countDistinct(person.lastName) { from(person) } shouldBeGreaterThan 0L
        }
    }

    @Test
    fun `direct write overloads build and execute providers`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
            val record = Person(900, "Direct", "Insert", java.time.LocalDate.now(), true, "Engineer", 1)
            conn.insert(record) {
                into(person)
                map(person.id) toProperty Person::id.name
                map(person.firstName) toProperty Person::firstName.name
                map(person.lastName) toProperty Person::lastName.name
                map(person.birthDate) toProperty Person::birthDate.name
                map(person.employed) toProperty Person::employed.name
                map(person.occupation) toProperty Person::occupation.name
                map(person.addressId) toProperty Person::addressId.name
            }.rowCount() shouldBeEqualTo 1

            val records = listOf(
                record.copy(id = 901),
                record.copy(id = 902),
            )
            conn.insertMultiple(records = records, completer = {
                into(person)
                map(person.id) toProperty Person::id.name
                map(person.firstName) toProperty Person::firstName.name
                map(person.lastName) toProperty Person::lastName.name
                map(person.birthDate) toProperty Person::birthDate.name
                map(person.employed) toProperty Person::employed.name
                map(person.occupation) toProperty Person::occupation.name
                map(person.addressId) toProperty Person::addressId.name
            }).rowCount() shouldBeEqualTo 2

            conn.insertMultiple(record.copy(id = 903), record.copy(id = 904), completer = {
                into(person)
                map(person.id) toProperty Person::id.name
                map(person.firstName) toProperty Person::firstName.name
                map(person.lastName) toProperty Person::lastName.name
                map(person.birthDate) toProperty Person::birthDate.name
                map(person.employed) toProperty Person::employed.name
                map(person.occupation) toProperty Person::occupation.name
                map(person.addressId) toProperty Person::addressId.name
            }).rowCount() shouldBeEqualTo 2

            conn.generalInsert(person) {
                set(person.id).toValue(905)
                set(person.firstName).toValue("General")
                set(person.lastName).toValue("Insert")
                set(person.birthDate).toValue(java.time.LocalDate.now())
                set(person.employed).toValue(true)
                set(person.occupation).toValue("Engineer")
                set(person.addressId).toValue(1)
            }.rowCount() shouldBeEqualTo 1

            conn.deleteFrom(person) { where { person.id isEqualTo 905 } }.rowCount() shouldBeEqualTo 1
        }
    }

    @Test
    fun `update set to subquery`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
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

    private data class NameRecord(
        var id: Int = 0,
        var name: String = "",
    )
}

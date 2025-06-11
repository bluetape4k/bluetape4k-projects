package io.bluetape4k.vertx.sqlclient.mybatis

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.trimWhitespace
import io.bluetape4k.vertx.sqlclient.AbstractVertxSqlClientTest
import io.bluetape4k.vertx.sqlclient.getGeneratedId
import io.bluetape4k.vertx.sqlclient.schema.GeneratedAlwaysTable.generatedAlways
import io.bluetape4k.vertx.sqlclient.schema.Person
import io.bluetape4k.vertx.sqlclient.schema.PersonAddressMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.address
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.person
import io.bluetape4k.vertx.sqlclient.templates.toParameters
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendRollback
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.SqlResult
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mybatis.dynamic.sql.insert.render.GeneralInsertStatementProvider
import org.mybatis.dynamic.sql.insert.render.InsertSelectStatementProvider
import org.mybatis.dynamic.sql.util.kotlin.KInvalidSQLException
import org.mybatis.dynamic.sql.util.kotlin.elements.add
import org.mybatis.dynamic.sql.util.kotlin.elements.constant
import org.mybatis.dynamic.sql.util.kotlin.elements.upper
import org.mybatis.dynamic.sql.util.kotlin.model.count
import org.mybatis.dynamic.sql.util.kotlin.model.countDistinct
import org.mybatis.dynamic.sql.util.kotlin.model.countFrom
import org.mybatis.dynamic.sql.util.kotlin.model.deleteFrom
import org.mybatis.dynamic.sql.util.kotlin.model.insert
import org.mybatis.dynamic.sql.util.kotlin.model.insertBatch
import org.mybatis.dynamic.sql.util.kotlin.model.insertInto
import org.mybatis.dynamic.sql.util.kotlin.model.insertMultiple
import org.mybatis.dynamic.sql.util.kotlin.model.insertSelect
import org.mybatis.dynamic.sql.util.kotlin.model.select
import org.mybatis.dynamic.sql.util.kotlin.model.selectDistinct
import org.mybatis.dynamic.sql.util.kotlin.model.update
import java.io.Serializable
import java.time.LocalDate
import kotlin.test.assertFailsWith

abstract class AbstractSqlClientExtensionsTest: AbstractVertxSqlClientTest() {

    companion object: KLoggingChannel()

    override val schemaFileNames: List<String> = listOf("person.sql", "generatedAlways.sql")

    @Nested
    inner class CountTest {
        @Test
        fun `raw count`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val countStatement = countFrom(person) {
                    where { person.id isLessThan 3 }
                }.renderForVertx()

                countStatement.selectStatement shouldBeEqualTo
                        "select count(*) from Person where id < #{p1}"

                val count = conn.count(countStatement)
                count shouldBeEqualTo 2L
            }
        }

        @Test
        fun `raw count all rows`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val countStatement = countFrom(person) {
                    allRows()
                }.renderForVertx()

                countStatement.selectStatement shouldBeEqualTo
                        "select count(*) from Person"

                val count = conn.count(countStatement)
                count shouldBeGreaterThan 0L
            }
        }

        @Test
        fun `raw count last name`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val countStatement = count(person.lastName) {
                    from(person)
                }.renderForVertx()

                countStatement.selectStatement shouldBeEqualTo
                        "select count(last_name) from Person"

                val count = conn.count(countStatement)
                count shouldBeGreaterThan 0L
            }
        }

        @Test
        fun `raw count distinct last name`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val countStatement = countDistinct(person.lastName) {
                    from(person)
                }.renderForVertx()

                countStatement.selectStatement shouldBeEqualTo
                        "select count(distinct last_name) from Person"

                val count = conn.count(countStatement)
                count shouldBeGreaterThan 0L
            }
        }
    }

    @Nested
    inner class DeleteTest {

        @Test
        fun `delete by id`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val deleteProvider = deleteFrom(person) {
                    where { person.id isLessThan 3 }
                }.renderForVertx()

                deleteProvider.deleteStatement shouldBeEqualTo
                        "delete from Person where id < #{p1}"

                val result = conn.delete(deleteProvider)
                result.rowCount() shouldBeGreaterThan 0
            }
        }

        @Test
        fun `delete 2`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val deleteProvider = deleteFrom(person) {
                    where {
                        person.id isLessThan 4
                        and { person.occupation.isNotNull() }
                    }
                }.renderForVertx()

                deleteProvider.deleteStatement shouldBeEqualTo
                        "delete from Person where id < #{p1} and occupation is not null"

                val result = conn.delete(deleteProvider)
                result.rowCount() shouldBeGreaterThan 0
            }
        }

        @Test
        fun `delete 3`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val deleteProvider = deleteFrom(person) {
                    where {
                        person.id isLessThan 4
                        or { person.occupation.isNotNull() }
                    }
                }.renderForVertx()

                deleteProvider.deleteStatement shouldBeEqualTo
                        "delete from Person where id < #{p1} or occupation is not null"

                val result = conn.delete(deleteProvider)
                result.rowCount() shouldBeGreaterThan 0
            }
        }

        @Test
        fun `delete 4`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val deleteProvider = deleteFrom(person) {
                    where {
                        group {
                            person.id isLessThan 4
                            or { person.occupation.isNotNull() }
                        }
                        and { person.employed isEqualTo true }
                    }
                    // and { person.employed isEqualTo true }
                }.renderForVertx()

                deleteProvider.deleteStatement shouldBeEqualTo
                        "delete from Person " +
                        "where (id < #{p1} or occupation is not null) " +
                        "and employed = #{p2}"

                val result = conn.delete(deleteProvider)
                result.rowCount() shouldBeGreaterThan 0
            }
        }

        @Test
        fun `delete 5`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val deleteProvider = deleteFrom(person) {
                    where {
                        person.id isLessThan 4
                        or {
                            person.occupation.isNotNull()
                            and { person.employed isEqualTo true }
                        }
                    }
                }.renderForVertx()

                deleteProvider.deleteStatement shouldBeEqualTo
                        "delete from Person " +
                        "where id < #{p1} " +
                        "or (occupation is not null and employed = #{p2})"

                val result = conn.delete(deleteProvider)
                result.rowCount() shouldBeGreaterThan 0
            }
        }

        @Test
        fun `delete 6`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val deleteProvider = deleteFrom(person) {
                    where {
                        person.id isLessThan 4
                        and {
                            person.occupation.isNotNull()
                            and { person.employed isEqualTo true }
                        }
                    }
                }.renderForVertx()

                deleteProvider.deleteStatement shouldBeEqualTo
                        "delete from Person " +
                        "where id < #{p1} " +
                        "and (occupation is not null and employed = #{p2})"

                val result = conn.delete(deleteProvider)
                result.rowCount() shouldBeGreaterThan 0
            }
        }
    }

    @Nested
    inner class InsertTest {

        @Test
        fun `insert record`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val record = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)

                val insertProvider = insert(record) {
                    into(person)
                    map(person.id) toProperty Person::id.name
                    map(person.firstName) toProperty Person::firstName.name
                    map(person.lastName) toProperty Person::lastName.name
                    map(person.birthDate) toProperty Person::birthDate.name
                    map(person.employed) toProperty Person::employed.name
                    map(person.occupation) toProperty Person::occupation.name
                    map(person.addressId) toProperty Person::addressId.name
                }.renderForVertx()

                log.debug { "SQL: ${insertProvider.insertStatement}" }

                insertProvider.insertStatement shouldBeEqualTo
                        "insert into Person (id, first_name, last_name, birth_date, employed, occupation, address_id) " +
                        "values (#{id}, #{firstName}, #{lastName}, #{birthDate}, #{employed}, #{occupation}, #{addressId})"

                val result = conn.insert(insertProvider)
                result.rowCount() shouldBeEqualTo 1
            }
        }

        @Test
        fun `insert into`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val insertProvider = insertInto(person) {
                    set(person.id) toConstant "100"                     // literal
                    set(person.firstName) toStringConstant "Joe"        // literal
                    set(person.lastName) toValue "Jones"                // parameter
                    set(person.birthDate) toValue LocalDate.now()
                    set(person.employed) toValue true
                    set(person.occupation).toNull()                     // liternal
                    set(person.addressId) toValue 1
                }.renderForVertx()

                log.debug { "SQL: ${insertProvider.insertStatement}" }

                insertProvider.insertStatement shouldBeEqualTo
                        "insert into Person (id, first_name, last_name, birth_date, employed, occupation, address_id) " +
                        "values (100, 'Joe', #{p1}, #{p2}, #{p3}, null, #{p4})"

                val result = conn.generalInsert(insertProvider)
                result.rowCount() shouldBeEqualTo 1

                val row = conn.selectOne(person.id, person.firstName, person.lastName) {
                    from(person)
                    where { person.id isEqualTo 100 }
                }!!
                row.getInteger("id") shouldBeEqualTo 100
            }
        }

        @Test
        fun `general insert special conditions`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val insertProvider = insertInto(person) {
                    set(person.id) toConstant "100"                     // literal
                    set(person.firstName) toStringConstant "Joe"        // literal
                    set(person.lastName) toValue "Jones"                // parameter
                    set(person.birthDate) toValue LocalDate.now()
                    set(person.employed) toValueOrNull true             // special condition
                    set(person.occupation) toValueWhenPresent null      // null 이므로 빠진다
                    set(person.addressId) toValue 1
                }.renderForVertx()

                insertProvider.insertStatement shouldBeEqualTo
                        "insert into Person (id, first_name, last_name, birth_date, employed, address_id) " +
                        "values (100, 'Joe', #{p1}, #{p2}, #{p3}, #{p4})"

                val result = conn.generalInsert(insertProvider)
                result.rowCount() shouldBeEqualTo 1

                val row = conn.selectOne(person.id, person.firstName, person.lastName) {
                    from(person)
                    where { person.id isEqualTo 100 }
                }!!
                row.getInteger("id") shouldBeEqualTo 100
            }
        }

        @Test
        fun `insert multi records`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val record1 = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)
                val record2 = Person(101, "Sarah", "Smith", LocalDate.now(), true, "Architect", 2)

                val insertProvider = insertMultiple(listOf(record1, record2)) {
                    into(person)
                    map(person.id) toProperty Person::id.name
                    map(person.firstName) toProperty Person::firstName.name
                    map(person.lastName) toProperty Person::lastName.name
                    map(person.birthDate) toProperty Person::birthDate.name
                    map(person.employed) toProperty Person::employed.name
                    map(person.occupation) toProperty Person::occupation.name
                    map(person.addressId) toProperty Person::addressId.name
                }.renderForVertx()

                log.debug { "SQL: ${insertProvider.insertStatement}" }
                log.debug { "Parameters: ${insertProvider.records.toParameters()}" }

                insertProvider.insertStatement shouldBeEqualTo
                        "insert into Person (id, first_name, last_name, birth_date, employed, occupation, address_id) " +
                        "values (#{id0}, #{firstName0}, #{lastName0}, #{birthDate0}, #{employed0}, #{occupation0}, #{addressId0}), " +
                        "(#{id1}, #{firstName1}, #{lastName1}, #{birthDate1}, #{employed1}, #{occupation1}, #{addressId1})"

                val result = conn.insertMultiple(insertProvider)
                result.rowCount() shouldBeEqualTo 2
            }
        }

        @Test
        fun `batch insert by batchInsert`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val record1 = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)
                val record2 = Person(101, "Sarah", "Smith", LocalDate.now(), true, "Architect", 2)

                val insertBatch = insertBatch(listOf(record1, record2)) {
                    into(person)
                    map(person.id) toProperty Person::id.name
                    map(person.firstName) toProperty Person::firstName.name
                    map(person.lastName) toProperty Person::lastName.name
                    map(person.birthDate) toProperty Person::birthDate.name
                    map(person.employed) toProperty Person::employed.name
                    map(person.occupation) toProperty Person::occupation.name
                    map(person.addressId) toProperty Person::addressId.name
                }.renderForVertx()

                log.debug { "SQL: ${insertBatch.insertStatementSQL}" }

                val result = conn.insertBatch(insertBatch)
                result.rowCount() shouldBeGreaterThan 0
            }
        }

        @Test
        fun `batch insert direct`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val record1 = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)
                val record2 = Person(101, "Sarah", "Smith", LocalDate.now(), true, "Architect", 2)

                val result = conn.insertBatch(record1, record2) {
                    into(person)
                    map(person.id) toProperty Person::id.name
                    map(person.firstName) toProperty Person::firstName.name
                    map(person.lastName) toProperty Person::lastName.name
                    map(person.birthDate) toProperty Person::birthDate.name
                    map(person.employed) toProperty Person::employed.name
                    map(person.occupation) toProperty Person::occupation.name
                    map(person.addressId) toProperty Person::addressId.name
                }

                log.debug { "result=$result" }
                result.rowCount() shouldBeGreaterThan 0
            }
        }

        @Test
        fun `insert select`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val insertProvider = insertSelect {
                    into(person)
                    columns(
                        person.id,
                        person.firstName,
                        person.lastName,
                        person.birthDate,
                        person.employed,
                        person.occupation,
                        person.addressId
                    )
                    select(
                        add(person.id, constant<Int>("100")),
                        person.firstName,
                        person.lastName,
                        person.birthDate,
                        person.employed,
                        person.occupation,
                        person.addressId
                    ) {
                        from(person)
                        orderBy(person.id)
                    }
                }.renderForVertx()

                insertProvider.insertStatement shouldBeEqualTo
                        "insert into Person (id, first_name, last_name, birth_date, employed, occupation, address_id) " +
                        "select (id + 100), first_name, last_name, birth_date, employed, occupation, address_id " +
                        "from Person " +
                        "order by id"

                val result = conn.insertSelect(insertProvider)

                log.debug { "rowCount=$result" }
                result.rowCount() shouldBeGreaterThan 0

                val rows = conn.select(person.id, person.firstName, person.lastName) {
                    from(person)
                    where { person.id isGreaterThanOrEqualTo 100 }
                    orderBy(person.id)
                }
                rows.size() shouldBeEqualTo 6
            }
        }

        @Test
        fun `insert select no columns`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) {
                assertFailsWith<KInvalidSQLException> {
                    insertSelect {
                        // columns(person.id, person.firstName, person.lastName, person.birthDate, person.employed, person.occupation, person.addressId)
                        select(
                            add(person.id, constant<Int>("100")),
                            person.firstName,
                            person.lastName,
                            person.birthDate,
                            person.employed,
                            person.occupation,
                            person.addressId
                        ) {
                            from(person)
                            orderBy(person.id)
                        }
                    }.renderForVertx()
                }
            }
        }

        @Test
        fun `insert select no select statement`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) {
                assertFailsWith<KInvalidSQLException> {
                    insertSelect {
                        into(person)
                        columns(
                            person.id,
                            person.firstName,
                            person.lastName,
                            person.birthDate,
                            person.employed,
                            person.occupation,
                            person.addressId
                        )
                        // select(add(person.id, constant<Int>("100")), person.firstName, person.lastName, person.birthDate, person.employed, person.occupation, person.addressId) {
                        //     from(person)
                        //     orderBy(person.id)
                        // }
                    }.renderForVertx()
                }
            }
        }

        @Test
        fun `batch insert no table`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->

                val record1 = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)
                val record2 = Person(101, "Sarah", "Smith", LocalDate.now(), true, "Architect", 2)

                assertFailsWith<KInvalidSQLException> {
                    conn.insertBatch(record1, record2) {
                        // into(person)
                        map(person.id) toProperty Person::id.name
                        map(person.firstName) toProperty Person::firstName.name
                        map(person.lastName) toProperty Person::lastName.name
                    }
                }
            }
        }

        @Test
        fun `insert record no table`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) {
                val record = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)

                assertFailsWith<KInvalidSQLException> {
                    insert(record) {
                        // into(person)
                        map(person.id) toProperty Person::id.name
                        map(person.firstName) toProperty Person::firstName.name
                        map(person.lastName) toProperty Person::lastName.name
                    }.renderForVertx()
                }
            }
        }

        @Test
        fun `insert multi records no table`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) {
                val record1 = Person(100, "Joe", "Jones", LocalDate.now(), true, "Developer", 1)
                val record2 = Person(101, "Sarah", "Smith", LocalDate.now(), true, "Architect", 2)

                assertFailsWith<KInvalidSQLException> {
                    insertMultiple(listOf(record1, record2)) {
                        // into(person)
                        map(person.id) toProperty Person::id.name
                        map(person.firstName) toProperty Person::firstName.name
                        map(person.lastName) toProperty Person::lastName.name
                        map(person.birthDate) toProperty Person::birthDate.name
                        map(person.employed) toProperty Person::employed.name
                        map(person.occupation) toProperty Person::occupation.name
                        map(person.addressId) toProperty Person::addressId.name
                    }.renderForVertx()
                }
            }
        }

        @Test
        fun `general insert with generated key`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val insertProvider: GeneralInsertStatementProvider = insertInto(generatedAlways) {
                    set(generatedAlways.firstName) toValue "Fred"
                    set(generatedAlways.lastName) toValue "Flintstone"
                }.renderForVertx()

                insertProvider.insertStatement shouldBeEqualTo
                        "insert into GeneratedAlways (first_name, last_name) values (#{p1}, #{p2})"

                val result: SqlResult<Void> = conn.generalInsert(insertProvider)

                // AUTO INCREMENT 컬럼의 값을 가져온다
                val generatedId = result.getGeneratedId<Long>(conn)
                log.debug { "generated id=$generatedId" }
                generatedId.shouldNotBeNull() shouldBeGreaterThan 0L

                result.rowCount() shouldBeEqualTo 1
            }
        }

        @Test
        fun `insert select with generated key`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val insertProvider: InsertSelectStatementProvider = insertSelect {
                    into(generatedAlways)
                    columns(generatedAlways.firstName, generatedAlways.lastName)
                    select(person.firstName, person.lastName) {
                        from(person)
                    }
                }.renderForVertx()

                insertProvider.insertStatement shouldBeEqualTo
                        "insert into GeneratedAlways (first_name, last_name) select first_name, last_name from Person"

                val result: SqlResult<Void> = conn.insertSelect(insertProvider)

                // AUTO INCREMENT 컬럼의 값을 가져온다
                val generatedId = result.getGeneratedId<Long>(conn)
                log.debug { "generated id=$generatedId" }
                generatedId.shouldNotBeNull() shouldBeGreaterThan 0L

                result.rowCount() shouldBeEqualTo 6
            }
        }
    }

    @Nested
    inner class SelectTest {

        @Test
        fun `raw select`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(
                    person.id alias "A_ID",
                    person.firstName,
                    person.lastName,
                    person.birthDate,
                    person.employed,
                    person.occupation,
                    person.addressId
                ) {
                    from(person)
                    where {
                        person.id isLessThan 4
                        and { person.occupation.isNotNull() }
                        and { person.occupation.isNotNull() }
                    }
                    orderBy(person.id)
                    limit(3)
                }.renderForVertx()

                val persons = conn.selectList(selectProvider, PersonMapper)

                persons.forEach { person ->
                    log.debug { "person=$person" }
                }
                persons shouldHaveSize 2
                persons.map { it.id } shouldBeEqualTo listOf(1, 2)
            }
        }

        @Test
        fun `raw select distinct`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = selectDistinct(person.lastName) {
                    from(person)
                }.renderForVertx()

                val lastNames = conn.selectList(selectProvider) { row ->
                    row.getString(0)
                }

                lastNames shouldHaveSize 2
                lastNames shouldBeEqualTo listOf("Flintstone", "Rubble")
            }
        }

        @Test
        fun `raw select with missing record`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.allColumns()) {
                    from(person)
                    where { person.id isEqualTo -1 }

                }.renderForVertx()

                val person = conn.selectOne(selectProvider, PersonMapper)
                person.shouldBeNull()
            }
        }

        @Test
        fun `raw select by primary key`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.allColumns()) {
                    from(person)
                    where { person.id isEqualTo 1 }

                }.renderForVertx()

                val person = conn.selectOne(selectProvider, PersonMapper)
                log.debug { "person=$person" }
                person.shouldNotBeNull()
                person.id shouldBeEqualTo 1
                person.firstName shouldBeEqualTo "Fred"
            }
        }

        @Test
        fun `raw select with union`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.allColumns()) {
                    from(person)
                    where { person.id isEqualTo 1 }
                    union {
                        select(person.allColumns()) {
                            from(person)
                            where { person.id isEqualTo 2 }
                        }
                    }
                    union {
                        select(person.allColumns()) {
                            from(person)
                            where { person.id isEqualTo 3 }
                        }
                    }
                    orderBy(person.id)
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select * from Person where id = #{p1} " +
                        "union select * from Person where id = #{p2} " +
                        "union select * from Person where id = #{p3} " +
                        "order by id"

                val persons = conn.selectList(selectProvider, PersonMapper)
                log.debug { "person=$persons" }
                persons shouldHaveSize 3
                persons.map { it.id }.sortedBy { it } shouldBeEqualTo listOf(1, 2, 3)
            }
        }

        @Test
        fun `raw select with union and alias`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.id alias "A_ID", person.firstName) {
                    from(person)
                    where { person.id isEqualTo 1 }
                    union {
                        select(person.id alias "A_ID", person.firstName) {
                            from(person)
                            where { person.id isEqualTo 2 }
                        }
                    }
                    union {
                        select(person.id alias "A_ID", person.firstName) {
                            from(person)
                            where { person.id isEqualTo 3 }
                        }
                    }
                    orderBy(person.id alias "A_ID")
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select id as A_ID, first_name from Person where id = #{p1} " +
                        "union select id as A_ID, first_name from Person where id = #{p2} " +
                        "union select id as A_ID, first_name from Person where id = #{p3} " +
                        "order by A_ID"

                val persons = conn.selectList(selectProvider, PersonMapper)
                log.debug { "person=$persons" }
                persons shouldHaveSize 3
                persons.map { it.id } shouldBeEqualTo listOf(1, 2, 3)
            }
        }

        @Test
        fun `raw select with union and distinct`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.id alias "A_ID", person.firstName) {
                    from(person)
                    where { person.id isEqualTo 1 }
                    union {
                        select(person.id alias "A_ID", person.firstName) {
                            from(person)
                            where { person.id isEqualTo 2 }
                        }
                    }
                    union {
                        selectDistinct(person.id alias "A_ID", person.firstName) {
                            from(person, "p")
                            where { person.id isEqualTo 3 }
                        }
                    }
                    orderBy(person.id alias "A_ID")
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select id as A_ID, first_name from Person where id = #{p1} " +
                        "union select id as A_ID, first_name from Person where id = #{p2} " +
                        "union select distinct p.id as A_ID, p.first_name from Person p where p.id = #{p3} " +
                        "order by A_ID"

                val persons = conn.selectList(selectProvider, PersonMapper)
                log.debug { "person=$persons" }
                persons shouldHaveSize 3
                persons.map { it.id } shouldBeEqualTo listOf(1, 2, 3)
            }
        }

        @Test
        fun `raw select with unionAll and distinct`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.id alias "A_ID", person.firstName) {
                    from(person)
                    where { person.id isEqualTo 1 }
                    union {
                        select(person.id alias "A_ID", person.firstName) {
                            from(person)
                            where { person.id isEqualTo 2 }
                        }
                    }
                    unionAll {
                        selectDistinct(person.id alias "A_ID", person.firstName) {
                            from(person, "p")
                            where { person.id isEqualTo 3 }
                        }
                    }
                    orderBy(person.id alias "A_ID")
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select id as A_ID, first_name from Person where id = #{p1} " +
                        "union select id as A_ID, first_name from Person where id = #{p2} " +
                        "union all select distinct p.id as A_ID, p.first_name from Person p where p.id = #{p3} " +
                        "order by A_ID"

                val persons = conn.selectList(selectProvider, PersonMapper)
                log.debug { "person=$persons" }
                persons shouldHaveSize 3
                persons.map { it.id } shouldBeEqualTo listOf(1, 2, 3)
            }
        }

        @Test
        fun `raw select with join`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(
                    person.id alias "A_ID",
                    person.firstName,
                    person.lastName,
                    person.birthDate,
                    person.employed,
                    person.occupation,
                    address.id,
                    address.streetAddress,
                    address.city,
                    address.state,
                ) {
                    from(person, "p")
                    join(address, "a") { on(person.addressId) equalTo address.id }
                    where { person.id isLessThan 4 }
                    orderBy(person.id)
                    limit(3)
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select p.id as A_ID, p.first_name, p.last_name, p.birth_date, p.employed, p.occupation, " +
                        "a.address_id, a.street_address, a.city, a.state " +
                        "from Person p " +
                        "join Address a on p.address_id = a.address_id " +
                        "where p.id < #{p1} " +
                        "order by id " +
                        "limit #{p2}"

                val personWithAddresses = conn.selectList(selectProvider, PersonAddressMapper)
                log.debug { personWithAddresses }
                personWithAddresses shouldHaveSize 3
                personWithAddresses.map { it.id } shouldBeEqualTo listOf(1, 2, 3)
            }
        }

        @Test
        fun `raw select with complex where 1`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.allColumns()) {
                    from(person)
                    where {
                        person.id isLessThan 5
                        and {
                            person.id isLessThan 4
                            and {
                                person.id isLessThan 3
                                and { person.id isLessThan 2 }
                            }
                        }
                    }
                    orderBy(person.id)
                    limit(3)
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select * from Person " +
                        "where id < #{p1} " +
                        "and (id < #{p2} and (id < #{p3} and id < #{p4})) " +
                        "order by id " +
                        "limit #{p5}"

                val persons = conn.selectList(selectProvider, PersonMapper)
                log.debug { persons }
                persons shouldHaveSize 1
                persons.map { it.id } shouldBeEqualTo listOf(1)
            }
        }

        @Test
        fun `raw select with complex where 2`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.allColumns()) {
                    from(person)
                    where {
                        person.id isEqualTo 5
                        or {
                            person.id isEqualTo 4
                            or {
                                person.id isEqualTo 3
                                or { person.id isEqualTo 2 }
                            }
                        }
                    }
                    orderBy(person.id)
                    limit(3)
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select * from Person " +
                        "where id = #{p1} " +
                        "or (id = #{p2} or (id = #{p3} or id = #{p4})) " +
                        "order by id " +
                        "limit #{p5}"

                val persons = conn.selectList(selectProvider, PersonMapper)
                log.debug { persons }
                persons shouldHaveSize 3   // limit 3
                persons.map { it.id } shouldBeEqualTo listOf(2, 3, 4)
            }
        }

        @Test
        fun `raw select with complex where in`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val selectProvider = select(person.allColumns()) {
                    from(person)
                    where { person.id.isIn(1, 3, 4, 5) }
                    orderBy(person.id)
                    limit(3)
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select * from Person " +
                        "where id in (#{p1},#{p2},#{p3},#{p4}) " +
                        "order by id " +
                        "limit #{p5}"

                val persons = conn.selectList(selectProvider, PersonMapper)
                log.debug { persons }
                persons shouldHaveSize 3   // limit 3
                persons.map { it.id } shouldBeEqualTo listOf(1, 3, 4)
            }
        }
    }

    @Nested
    inner class UpdateTest {

        @Test
        fun `raw update 1`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.lastName) equalTo "Smith"
                    where { person.firstName isEqualTo "Fred" }
                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set last_name = #{p1} " +
                        "where first_name = #{p2}"

                updateProvider.parameters shouldBeEqualTo mapOf("p1" to "Smith", "p2" to "Fred")

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 1
            }
        }

        @Test
        fun `raw update 1 - one step`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val result = conn.update(person) {
                    set(person.lastName) equalTo "Smith"
                    where { person.firstName isEqualTo "Fred" }
                }
                result.rowCount() shouldBeEqualTo 1
            }
        }

        @Test
        fun `raw update 2`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.firstName) equalTo "Sam"
                    where {
                        person.firstName isEqualTo "Fred"
                        or { person.id isGreaterThan 3 }
                    }
                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set first_name = #{p1} " +
                        "where first_name = #{p2} or id > #{p3}"

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 4
            }
        }

        @Test
        fun `raw update 3`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.firstName) equalTo "Sam"
                    where {
                        person.firstName isEqualTo "Fred"
                        or {
                            person.id isEqualTo 5
                            or { person.id isEqualTo 6 }
                        }
                    }

                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set first_name = #{p1} " +
                        "where first_name = #{p2} " +
                        "or (id = #{p3} or id = #{p4})"

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 3
            }
        }

        @Test
        fun `raw update 4`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.firstName) equalTo "Sam"
                    where {
                        person.firstName isEqualTo "Fred"
                        and {
                            person.id isEqualTo 1
                            or { person.id isEqualTo 6 }
                        }
                    }
                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set first_name = #{p1} " +
                        "where first_name = #{p2} " +
                        "and (id = #{p3} or id = #{p4})"

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 1
            }
        }

        @Test
        fun `raw update 5`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.firstName) equalTo "Sam"
                    where {
                        person.firstName isEqualTo "Fred"
                        or { person.id isEqualTo 3 }
                    }
                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set first_name = #{p1} " +
                        "where first_name = #{p2} " +
                        "or id = #{p3}"

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 2
            }
        }

        @Test
        fun `raw update 6`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.occupation) equalToOrNull null
                    where {
                        person.firstName isEqualTo "Fred"
                        or { person.id isEqualTo 3 }
                    }


                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set occupation = null " +
                        "where first_name = #{p1} " +
                        "or id = #{p2}"

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 2
            }
        }

        @Test
        fun `raw update 6 - one step`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val result = conn.update(person) {
                    set(person.occupation) equalToOrNull null
                    where {
                        person.firstName isEqualTo "Fred"
                        or { person.id isEqualTo 3 }
                    }
                }
                result.rowCount() shouldBeEqualTo 2
            }
        }

        @Test
        fun `update with type converter and null value`(vertx: Vertx, testContext: VertxTestContext) =
            runSuspendIO {
                vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                    val updateProvider = update(person) {
                        set(person.firstName) equalToOrNull "Sam"
                        set(person.lastName).equalToNull()
                        where { person.id isEqualTo 3 }

                    }.renderForVertx()

                    updateProvider.updateStatement shouldBeEqualTo
                            "update Person " +
                            "set first_name = #{p1}, last_name = null " +
                            "where id = #{p2}"

                    updateProvider.parameters shouldBeEqualTo mapOf("p1" to "Sam", "p2" to 3)

                    val result = conn.update(updateProvider)
                    result.rowCount() shouldBeEqualTo 1

                    val person = conn.selectOne(person.allColumns()) {
                        from(person)
                        where { person.id isEqualTo 3 }
                    }?.let { PersonMapper.map(it) }

                    person.shouldNotBeNull()
                    person.firstName shouldBeEqualTo "Sam"
                    person.lastName.shouldBeNull()
                }
            }

        @Test
        fun `update with type converter and non null value`(vertx: Vertx, testContext: VertxTestContext) =
            runSuspendIO {
                vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                    val updateProvider = update(person) {
                        set(person.firstName) equalTo "Sam"
                        set(person.lastName) equalTo "Smith"
                        where { person.id isEqualTo 3 }

                    }.renderForVertx()

                    updateProvider.updateStatement shouldBeEqualTo
                            "update Person " +
                            "set first_name = #{p1}, last_name = #{p2} " +
                            "where id = #{p3}"

                    updateProvider.parameters shouldBeEqualTo mapOf("p1" to "Sam", "p2" to "Smith", "p3" to 3)

                    val result = conn.update(updateProvider)
                    result.rowCount() shouldBeEqualTo 1

                    val person = conn.selectOne(listOf(person.allColumns()), PersonMapper) {
                        from(person)
                        where { person.id isEqualTo 3 }
                    }
                    person.shouldNotBeNull()
                    person.firstName shouldBeEqualTo "Sam"
                    person.lastName shouldBeEqualTo "Smith"
                }
            }

        @Test
        fun `update set null`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.addressId).equalToNull()
                    where { person.id isEqualTo 3 }

                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set address_id = null " +
                        "where id = #{p1}"

                updateProvider.parameters shouldBeEqualTo mapOf("p1" to 3)

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 1

                val person = conn.selectOne(listOf(person.allColumns()), PersonMapper) {
                    from(person)
                    where { person.id isEqualTo 3 }
                }
                person.shouldNotBeNull()
                person.addressId.shouldBeNull()
            }
        }

        @Test
        fun `update set to constant`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.addressId) equalToConstant "5"
                    where { person.id isEqualTo 3 }

                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set address_id = 5 " +
                        "where id = #{p1}"

                updateProvider.parameters shouldBeEqualTo mapOf("p1" to 3)

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 1

                val person = conn.selectOne(listOf(person.allColumns()), PersonMapper) {
                    from(person)
                    where { person.id isEqualTo 3 }
                }
                person.shouldNotBeNull()
                person.addressId shouldBeEqualTo 5
            }
        }


        @Test
        fun `update set to column`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.addressId) equalTo person.id
                    where { person.id isEqualTo 3 }

                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set address_id = id " +
                        "where id = #{p1}"

                updateProvider.parameters shouldBeEqualTo mapOf("p1" to 3)

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

        @Test
        fun `update set equalTo when present`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val updateProvider = update(person) {
                    set(person.addressId) equalTo 5
                    set(person.firstName) equalToWhenPresent null       // null 인 경우 set 에서 제외된다.
                    where { person.id isEqualTo 3 }

                }.renderForVertx()

                updateProvider.updateStatement shouldBeEqualTo
                        "update Person " +
                        "set address_id = #{p1} " +
                        "where id = #{p2}"

                updateProvider.parameters shouldBeEqualTo mapOf("p1" to 5, "p2" to 3)

                val result = conn.update(updateProvider)
                result.rowCount() shouldBeEqualTo 1

                val person = conn.selectOne(listOf(person.allColumns()), PersonMapper) {
                    from(person)
                    where { person.id isEqualTo 3 }
                }
                person.shouldNotBeNull()
                person.addressId shouldBeEqualTo 5
            }
        }
    }

    data class SearchParameters(
        val id: Int?,
        val firstName: String?,
        val lastName: String?,
    ): Serializable

    @Nested
    inner class SearchTest {
        @Test
        fun `update set equalTo when present`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
            vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
                val search1 = SearchParameters(id = null, firstName = "f", lastName = null)

                val selectProvider = select(person.allColumns()) {
                    from(person)
                    where {
                        person.id isEqualToWhenPresent search1.id
                        and {
                            val firstNameLike = search1.firstName?.trimWhitespace()?.uppercase()
                            upper(person.firstName) isLikeWhenPresent firstNameLike?.let { "%$it%" }
                        }
                        and {
                            val lastNameLike = search1.lastName?.trimWhitespace()?.uppercase()
                            upper(person.lastName) isLikeWhenPresent lastNameLike?.let { "%$it%" }
                        }
                    }
                    orderBy(person.id)
                    limit(3L)
                }.renderForVertx()

                selectProvider.selectStatement shouldBeEqualTo
                        "select * from Person " +
                        "where upper(first_name) like #{p1} " +
                        "order by id " +
                        "limit #{p2}"

                selectProvider.parameters shouldBeEqualTo mapOf("p1" to "%F%", "p2" to 3L)

                val persons = conn.selectList(selectProvider, PersonMapper)
                persons shouldHaveSize 1
                persons[0].id shouldBeEqualTo 1
                persons[0].firstName shouldBeEqualTo "Fred"
            }
        }
    }
}

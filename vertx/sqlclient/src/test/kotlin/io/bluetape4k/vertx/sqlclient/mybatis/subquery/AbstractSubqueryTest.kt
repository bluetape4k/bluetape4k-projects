package io.bluetape4k.vertx.sqlclient.mybatis.subquery

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.sqlclient.AbstractVertxSqlClientTest
import io.bluetape4k.vertx.sqlclient.mybatis.renderForVertx
import io.bluetape4k.vertx.sqlclient.mybatis.selectList
import io.bluetape4k.vertx.sqlclient.mybatis.selectOne
import io.bluetape4k.vertx.sqlclient.schema.PersonMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.person
import io.bluetape4k.vertx.sqlclient.tests.testWithRollbackSuspending
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlConnection
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.mybatis.dynamic.sql.util.kotlin.elements.max
import org.mybatis.dynamic.sql.util.kotlin.elements.min
import org.mybatis.dynamic.sql.util.kotlin.model.select

abstract class AbstractSubqueryTest: AbstractVertxSqlClientTest() {

    companion object: KLogging()

    override val schemaFileNames: List<String> = listOf("person.sql", "generatedAlways.sql")

    @Test
    fun `select not equal sub query`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isNotEqualTo { select(max(person.id)) { from(person) } }
                }
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person where id <> (select max(id) from Person)"

            val persons = conn.selectList(selectProvider, PersonMapper)
            persons shouldHaveSize 5
        }
    }

    @Test
    fun `select equal sub query 2`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isEqualTo {
                        select(max(person.id)) {
                            from(person)
                        }
                    }
                }
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person where id = (select max(id) from Person)"

            val person = conn.selectOne(selectProvider, PersonMapper)
            person.shouldNotBeNull()
            person.id shouldBeEqualTo 6
            person.firstName shouldBeEqualTo "Bamm Bamm"
        }
    }

    @Test
    fun `in sub query`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isIn {
                        select(person.id) {
                            from(person)
                            where { person.lastName isEqualTo "Rubble" }
                        }
                    }
                }
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person " +
                    "where id in (select id from Person where last_name = #{p1})"

            val persons = conn.selectList(selectProvider, PersonMapper)
            persons.forEach { log.debug { it } }
            persons shouldHaveSize 3
            persons.map { it.id } shouldBeEqualTo listOf(4, 5, 6)
        }
    }

    @Test
    fun `not in sub query`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isNotIn {
                        selectDistinct(person.id) {
                            from(person)
                            where { person.lastName isEqualTo "Rubble" }
                        }
                    }
                }
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person " +
                    "where id not in (select distinct id from Person where last_name = #{p1})"

            val persons = conn.selectList(selectProvider, PersonMapper)
            persons.forEach { log.debug { it } }
            persons shouldHaveSize 3
            persons.map { it.id } shouldBeEqualTo listOf(1, 2, 3)
        }
    }

    @Test
    fun `less than sub query`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isLessThan { select(max(person.id)) { from(person) } }
                }
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person " +
                    "where id < (select max(id) from Person)"

            val persons = conn.selectList(selectProvider, PersonMapper)
            persons.forEach { log.debug { it } }
            persons shouldHaveSize 5
            persons.map { it.id } shouldBeEqualTo listOf(1, 2, 3, 4, 5)
        }
    }

    @Test
    fun `less than or equal sub query`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isLessThanOrEqualTo { select(max(person.id)) { from(person) } }
                }
                orderBy(person.id)
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person " +
                    "where id <= (select max(id) from Person) " +
                    "order by id"

            val persons = conn.selectList(selectProvider, PersonMapper)
            persons.forEach { log.debug { it } }
            persons shouldHaveSize 6
            persons.map { it.id } shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6)
        }
    }

    @Test
    fun `greater than sub query`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isGreaterThan { select(min(person.id)) { from(person) } }
                }
                orderBy(person.id)
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person " +
                    "where id > (select min(id) from Person) " +
                    "order by id"

            val persons = conn.selectList(selectProvider, PersonMapper)
            persons.forEach { log.debug { it } }
            persons shouldHaveSize 5
            persons.map { it.id } shouldBeEqualTo listOf(2, 3, 4, 5, 6)
        }
    }

    @Test
    fun `greater than or equal sub query`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.testWithRollbackSuspending(testContext, pool) { conn: SqlConnection ->
            val selectProvider = select(person.allColumns()) {
                from(person)
                where {
                    person.id isGreaterThanOrEqualTo { select(min(person.id)) { from(person) } }
                }
                orderBy(person.id)
            }.renderForVertx()

            selectProvider.selectStatement shouldBeEqualTo
                    "select * from Person " +
                    "where id >= (select min(id) from Person) " +
                    "order by id"

            val persons = conn.selectList(selectProvider, PersonMapper)
            persons.forEach { log.debug { it } }
            persons shouldHaveSize 6
            persons.map { it.id } shouldBeEqualTo listOf(1, 2, 3, 4, 5, 6)
        }
    }
}

package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.datastax.oss.driver.api.querybuilder.relation.Relation
import com.datastax.oss.driver.api.querybuilder.select.Selector
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class SelectFromStatementExamples {

    companion object: KLoggingChannel()

    @Test
    fun `create SimpleStatement from Select`() {
        val stmt = selectFrom("examples", "persons").all().build()
        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM examples.persons"
    }

    @Test
    fun `create SimpleStatement with ordering`() {
        val stmt = selectFrom("persons").all().orderBy("foo", ClusteringOrder.ASC).build()
        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons ORDER BY foo ASC"
    }

    @Test
    fun `create SimpleStatement with bind function and allow filtering`() {
        val stmt = selectFrom("persons").all()
            // .where(Relation.column("foo").eq("bar".literal()))
            .whereColumn("foo").eq("bar".literal())
            .allowFiltering()
            .build()

        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo='bar' ALLOW FILTERING"
    }

    @Test
    fun `create SimpleStatement with bind by index`() {
        val stmt = selectFrom("persons").all()
            .where(Relation.column("foo").eq(QueryBuilder.bindMarker()))
            .build("bar")

        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo=?"
        log.debug { "positionalValues=${stmt.positionalValues}" }
        stmt.positionalValues shouldBeEqualTo listOf("bar")
    }

    @Test
    fun `create SimpleStatement with bind by name`() {
        val params = mapOf<String, Any>("foo" to "bar", "name" to "debop")

        val stmt = selectFrom("persons").all()
            .where(Relation.column("foo").eq("foo".bindMarker()))
            .where(Relation.column("name").eq("name".bindMarker()))
            .build(params)

        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo=:foo AND name=:name"
        log.debug { "positionalValues=${stmt.namedValues}" }
        stmt.namedValues.map { it.key.asInternal() to it.value }.toMap() shouldContainSame params
    }

    @Test
    fun `create SimpleStatment with bind list`() {
        val stmt = selectFrom("persons").all()
            .where(Relation.column("foo").eq(listOf("value").literal()))
            .build()

        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo=['value']"
    }

    @Test
    fun `create SimpleStatment with bind set`() {
        val stmt = selectFrom("persons").all()
            .where(Relation.column("foo").eq(setOf("value").literal()))
            .build()

        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo={'value'}"
    }

    @Test
    fun `create SimpleStatment with bind map`() {
        val stmt = selectFrom("persons").all()
            .where(Relation.column("foo").eq(mapOf("key" to "value").literal()))
            .build()

        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo={'key':'value'}"
    }


    @Test
    fun `create SimpleStatement with bind function and odering`() {
        val stmt = selectFrom("persons").all()
            .whereColumn("foo").eq("bar".literal())
            .orderBy("one", ClusteringOrder.ASC)
            .whereColumn("bar").eq("baz".literal())
            .orderBy("two", ClusteringOrder.DESC)
            .build()

        log.debug { "query=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo='bar' AND bar='baz' ORDER BY one ASC,two DESC"
    }

    @Test
    fun `create SimpleStatement with in clauses`() {
        val stmt = selectFrom("persons").all()
            .whereColumn("foo").inValues("bar".literal(), "baz".literal())
            .build()

        log.debug { "stmt=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT * FROM persons WHERE foo IN ('bar','baz')"
    }

    @Test
    fun `create SimpleStatement with in clauses and bindMark`() {
        val stmt = selectFrom("persons")
            .column("name")
            .function("sum", Selector.column("age"))
            .whereColumn("foo").inValues("foo".bindMarker())
            .build(mapOf<String, Any>("foo" to listOf("bar", "baz")))

        log.debug { "stmt=${stmt.query}" }
        stmt.query shouldBeEqualTo "SELECT name,sum(age) FROM persons WHERE foo IN :foo"
    }
}

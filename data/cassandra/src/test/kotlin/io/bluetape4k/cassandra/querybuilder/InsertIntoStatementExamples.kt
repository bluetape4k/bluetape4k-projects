package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test

class InsertIntoStatementExamples {

    companion object: KLogging()

    @Test
    fun `generate column assignments`() {
        insertInto("foo")
            .value("a", 1.literal())
            .value("b", 2.literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (a,b) VALUES (1,2)"

        insertInto("foo")
            .value("a", 1.literal())
            .value("b", "bar".literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (a,b) VALUES (1,'bar')"

        insertInto("foo")
            .value("a", 1.literal())
            .value("b", "debop's cake".literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (a,b) VALUES (1,'debop''s cake')"

        insertInto("foo")
            .value("a", QueryBuilder.bindMarker())
            .value("b", "b".bindMarker())
            .asCql() shouldBeEqualTo "INSERT INTO foo (a,b) VALUES (?,:b)"
    }

    @Test
    fun `keep last assignment if column listed twice`() {
        insertInto("foo")
            .value("a", QueryBuilder.bindMarker())
            .value("b", QueryBuilder.bindMarker())
            .value("a", 1.literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (b,a) VALUES (?,1)"
    }

    @Test
    fun `generate bulk column assignments`() {
        val assignments = mapOf("a" to 1.literal(), "b" to "bar".literal())

        insertInto("ks", "foo")
            .values(assignments)
            .asCql() shouldBeEqualTo "INSERT INTO ks.foo (a,b) VALUES (1,'bar')"

        insertInto("ks", "foo")
            .value("a", 2.literal())
            .value("c", 3.literal())
            .values(assignments)
            .asCql() shouldBeEqualTo "INSERT INTO ks.foo (c,a,b) VALUES (3,1,'bar')"
    }

    @Test
    fun `generate if not exists clause`() {
        insertInto("foo").value("a", QueryBuilder.bindMarker()).ifNotExists()
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) IF NOT EXISTS"
    }

    @Test
    fun `generate using timestamp clause`() {
        insertInto("foo").value("a", QueryBuilder.bindMarker()).usingTimestamp(1)
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) USING TIMESTAMP 1"

        insertInto("foo").value("a", QueryBuilder.bindMarker()).usingTimestamp(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) USING TIMESTAMP ?"

        insertInto("foo").value("a", QueryBuilder.bindMarker()).usingTimestamp("ts".bindMarker())
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) USING TIMESTAMP :ts"

        insertInto("foo")
            .value("a", QueryBuilder.bindMarker())
            .usingTimestamp(1)
            .usingTimestamp(2)
            .usingTimestamp(3)
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) USING TIMESTAMP 3"

        insertInto("foo")
            .value("a", QueryBuilder.bindMarker())
            .ifNotExists()
            .usingTimestamp(1)
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) IF NOT EXISTS USING TIMESTAMP 1"
    }

    @Test
    fun `generate llt clause`() {
        insertInto("foo")
            .value("a", QueryBuilder.bindMarker())
            .usingTtl(10)
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) USING TTL 10"

        insertInto("foo")
            .value("a", QueryBuilder.bindMarker())
            .usingTtl(10)
            .usingTtl(20)
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) USING TTL 20"

        insertInto("foo")
            .value("a", QueryBuilder.bindMarker())
            .ifNotExists()
            .usingTtl(10)
            .usingTimestamp(30L)
            .asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (?) IF NOT EXISTS USING TIMESTAMP 30 AND TTL 10"

        with(
            insertInto("foo")
                .value("a", "a".bindMarker())
                .ifNotExists()
                .usingTtl("ttl".bindMarker())
                .usingTimestamp("ts".bindMarker())
        ) {

            asCql() shouldBeEqualTo "INSERT INTO foo (a) VALUES (:a) IF NOT EXISTS USING TIMESTAMP :ts AND TTL :ttl"

            build().isIdempotent!!.shouldBeFalse()
        }
    }

    @Test
    fun `generate with function term`() {
        insertInto("foo")
            .value("k", QueryBuilder.function("generate_id"))
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES (generate_id())"
    }

    @Test
    fun `generate with raw term`() {
        insertInto("foo")
            .value("k", "generate_id()".raw())
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES (generate_id())"
    }

    @Test
    fun `generate with term arthmetics`() {
        insertInto("foo")
            .value("k", 1.literal() + 2.literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES (1+2)"

        insertInto("foo")
            .value("k", 1.literal() + QueryBuilder.function("generate_id"))
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES (1+generate_id())"
    }

    @Test
    fun `generate with tuple`() {
        insertInto("foo")
            .value("k", QueryBuilder.tuple(1.literal(), 2.literal()))
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES ((1,2))"

        insertInto("foo")
            .value("k", QueryBuilder.tuple(1.literal(), QueryBuilder.function("generate_id")))
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES ((1,generate_id()))"
    }

    @Test
    fun `generate with list term`() {
        insertInto("foo")
            .value("k", listOf(1, 2).literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES ([1,2])"
    }

    @Test
    fun `generate with set term`() {
        insertInto("foo")
            .value("k", setOf(1, 2).literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES ({1,2})"
    }

    @Test
    fun `generate with map term`() {
        insertInto("foo")
            .value("k", mapOf("a" to 1, "b" to 2).literal())
            .asCql() shouldBeEqualTo "INSERT INTO foo (k) VALUES ({'a':1,'b':2})"
    }

    @Test
    fun `generate insert json`() {
        insertInto("foo")
            .json("""{"bar": 1}""")
            .asCql() shouldBeEqualTo """INSERT INTO foo JSON '{"bar": 1}'"""

        insertInto("foo")
            .json("json".bindMarker())
            .asCql() shouldBeEqualTo """INSERT INTO foo JSON :json"""

        insertInto("foo")
            .json("json".bindMarker()).defaultNull()
            .asCql() shouldBeEqualTo """INSERT INTO foo JSON :json DEFAULT NULL"""

        insertInto("foo")
            .json("json".bindMarker()).defaultUnset()
            .asCql() shouldBeEqualTo """INSERT INTO foo JSON :json DEFAULT UNSET"""

        insertInto("foo")
            .json("json".bindMarker()).defaultNull().defaultUnset()
            .asCql() shouldBeEqualTo """INSERT INTO foo JSON :json DEFAULT UNSET"""

        insertInto("foo")
            .json(QueryBuilder.bindMarker())
            .ifNotExists()
            .usingTtl(10)
            .usingTimestamp(30L)
            .asCql() shouldBeEqualTo "INSERT INTO foo JSON ? IF NOT EXISTS USING TIMESTAMP 30 AND TTL 10"
    }
}

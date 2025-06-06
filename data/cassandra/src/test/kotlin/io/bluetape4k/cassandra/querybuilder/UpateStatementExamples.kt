package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.update
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class UpateStatementExamples {

    companion object: KLoggingChannel()

    @Test
    fun `generate simple column assignment`() {
        update("foo")
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET v=? WHERE k=?"
    }

    @Test
    fun `generate field assignment`() {
        update("foo")
            .setField("address", "street", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET address.street=? WHERE k=?"
    }

    @Test
    fun `generate map value assignment`() {
        update("foo")
            .setMapValue("features", "color".literal(), QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET features['color']=? WHERE k=?"

        update("foo")
            .setMapValue("features", 1.literal(), QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET features[1]=? WHERE k=?"
    }

    @Test
    fun `generate counter operations`() {
        update("foo")
            .increment("c")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c+1 WHERE k=?"

        update("foo")
            .increment("c", 3.literal())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c+3 WHERE k=?"

        update("foo")
            .increment("c", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c+? WHERE k=?"

        update("foo")
            .increment("c", "inc".bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c+:inc WHERE k=?"

        update("foo")
            .decrement("c")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c-1 WHERE k=?"

        update("foo")
            .decrement("c", 3.literal())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c-3 WHERE k=?"

        update("foo")
            .decrement("c", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c-? WHERE k=?"

        update("foo")
            .decrement("c", "inc".bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET c=c-:inc WHERE k=?"
    }

    @Test
    fun `generate list operations`() {
        val listLiteral = listOf(1, 2, 3).literal()

        update("foo")
            .append("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+? WHERE k=?"

        update("foo")
            .append("m", listLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+[1,2,3] WHERE k=?"

        update("foo")
            .append("m", QueryBuilder.literal(listOf(1, 2, 3)))
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+[1,2,3] WHERE k=?"

        update("foo")
            .appendListElement("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+[?] WHERE k=?"

        update("foo")
            .prepend("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=?+m WHERE k=?"

        update("foo")
            .prepend("m", listLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=[1,2,3]+m WHERE k=?"

        update("foo")
            .prependListElement("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=[?]+m WHERE k=?"


        update("foo")
            .remove("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-? WHERE k=?"

        update("foo")
            .remove("m", listLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-[1,2,3] WHERE k=?"

        update("foo")
            .removeListElement("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-[?] WHERE k=?"
    }

    @Test
    fun `generate set operations`() {
        val setLiteral = setOf(1, 2, 3).literal()

        update("foo")
            .append("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+? WHERE k=?"

        update("foo")
            .append("m", setLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+{1,2,3} WHERE k=?"

        update("foo")
            .append("m", QueryBuilder.literal(setOf(1, 2, 3)))
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+{1,2,3} WHERE k=?"

        update("foo")
            .appendSetElement("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+{?} WHERE k=?"

        update("foo")
            .prepend("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=?+m WHERE k=?"

        update("foo")
            .prepend("m", setLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m={1,2,3}+m WHERE k=?"

        update("foo")
            .prependSetElement("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m={?}+m WHERE k=?"


        update("foo")
            .remove("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-? WHERE k=?"

        update("foo")
            .remove("m", setLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-{1,2,3} WHERE k=?"

        update("foo")
            .removeSetElement("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-{?} WHERE k=?"
    }

    @Test
    fun `generate map operations`() {
        val mapLiteral = mapOf(1 to "foo", 2 to "bar").literal()

        update("foo")
            .append("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+? WHERE k=?"

        update("foo")
            .append("m", mapLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+{1:'foo',2:'bar'} WHERE k=?"

        update("foo")
            .append("m", QueryBuilder.literal(mapOf(1 to "foo", 2 to "bar")))
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+{1:'foo',2:'bar'} WHERE k=?"

        update("foo")
            .appendMapEntry("m", 1.literal(), "foo".literal())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m+{1:'foo'} WHERE k=?"

        update("foo")
            .prepend("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=?+m WHERE k=?"

        update("foo")
            .prepend("m", mapLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m={1:'foo',2:'bar'}+m WHERE k=?"

        update("foo")
            .prependMapEntry("m", 1.literal(), "foo".literal())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m={1:'foo'}+m WHERE k=?"


        update("foo")
            .remove("m", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-? WHERE k=?"

        update("foo")
            .remove("m", mapLiteral)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-{1:'foo',2:'bar'} WHERE k=?"

        update("foo")
            .removeMapEntry("m", 1.literal(), "foo".literal())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET m=m-{1:'foo'} WHERE k=?"
    }

    @Test
    fun `generate simple column condition`() {
        update("foo")
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifColumn("v").eq(1.literal())
            .asCql() shouldBeEqualTo "UPDATE foo SET v=? WHERE k=? IF v=1"

        update("foo")
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifColumn("v1").eq(1.literal())
            .ifColumn("v2").eq("bar".literal())
            .asCql() shouldBeEqualTo "UPDATE foo SET v=? WHERE k=? IF v1=1 AND v2='bar'"
    }

    @Test
    fun `generate element condition`() {
        update("foo")
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifElement("v", 1.literal()).eq(2.literal())
            .asCql() shouldBeEqualTo "UPDATE foo SET v=? WHERE k=? IF v[1]=2"
    }

    @Test
    fun `generate if exists condition`() {
        update("foo")
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifExists()
            .asCql() shouldBeEqualTo "UPDATE foo SET v=? WHERE k=? IF EXISTS"
    }

    @Test
    fun `generate using function`() {
        update("foo")
            .setColumn("v", QueryBuilder.function("generate_id"))
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo SET v=generate_id() WHERE k=?"
    }


    @Test
    fun `generate if relation does not have right operand`() {
        update("foo")
            .setColumn("col1", 42.literal())
            .whereColumn("col2").isNotNull
            .asCql() shouldBeEqualTo "UPDATE foo SET col1=42 WHERE col2 IS NOT NULL"
    }

    @Test
    fun `generate using timestamp clause`() {
        update("foo")
            .usingTimestamp(1)
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo USING TIMESTAMP 1 SET v=? WHERE k=?"
    }

    @Test
    fun `generate using ttl clause`() {
        update("foo")
            .usingTtl(10)
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo USING TTL 10 SET v=? WHERE k=?"

        update("foo")
            .usingTtl(QueryBuilder.bindMarker())
            .setColumn("v", QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "UPDATE foo USING TTL ? SET v=? WHERE k=?"
    }
}

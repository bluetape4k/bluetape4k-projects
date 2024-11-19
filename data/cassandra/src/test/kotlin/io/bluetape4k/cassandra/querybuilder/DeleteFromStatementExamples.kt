package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom
import com.datastax.oss.driver.api.querybuilder.relation.Relation
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DeleteFromStatementExamples {

    companion object: KLogging()

    @Test
    fun `generate column deletion`() {
        deleteFrom("foo")
            .column("v")
            .whereColumn("k").eq("k".bindMarker())
            .asCql() shouldBeEqualTo "DELETE v FROM foo WHERE k=:k"

        deleteFrom("ks", "foo")
            .column("v")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "DELETE v FROM ks.foo WHERE k=?"
    }

    @Test
    fun `generate field deletion`() {
        deleteFrom("foo")
            .field("address", "street")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql().trim() shouldBeEqualTo "DELETE address.street FROM foo WHERE k=?"

        deleteFrom("foo")
            .field("address", "street")
            .field("address", "zip")
            .whereColumn("k").eq("k".bindMarker())
            .asCql().trim() shouldBeEqualTo "DELETE address.street,address.zip FROM foo WHERE k=:k"

        deleteFrom("foo")
            .column("v")
            .field("address", "street")
            .whereColumn("k").eq("k".bindMarker())
            .asCql().trim() shouldBeEqualTo "DELETE v,address.street FROM foo WHERE k=:k"
    }

    @Test
    fun `generate element deletion`() {
        deleteFrom("foo")
            .element("m", 1.literal())
            .whereColumn("k").isEqualTo(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "DELETE m[1] FROM foo WHERE k=?"

        deleteFrom("foo")
            .element("m", 1.literal())
            .where(Relation.columns("k", "m").eq(QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "DELETE m[1] FROM foo WHERE (k,m)=?"
    }

    @Test
    fun `generate using timestamp caluse`() {
        deleteFrom("foo")
            .usingTimestamp(1)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "DELETE FROM foo USING TIMESTAMP 1 WHERE k=?"

        deleteFrom("foo")
            .usingTimestamp(QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "DELETE FROM foo USING TIMESTAMP ? WHERE k=?"

        deleteFrom("foo")
            .column("v")
            .usingTimestamp(1)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "DELETE v FROM foo USING TIMESTAMP 1 WHERE k=?"

        deleteFrom("foo")
            .column("v")
            .usingTimestamp(QueryBuilder.bindMarker())
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "DELETE v FROM foo USING TIMESTAMP ? WHERE k=?"
    }

    @Test
    fun `use last timestamp if called multiple times`() {
        deleteFrom("foo")
            .usingTimestamp(1)
            .usingTimestamp(2)
            .usingTimestamp(3)
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .asCql() shouldBeEqualTo "DELETE FROM foo USING TIMESTAMP 3 WHERE k=?"
    }
}

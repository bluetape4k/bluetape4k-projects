package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom
import com.datastax.oss.driver.api.querybuilder.condition.Condition
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ConditionQuryExamples {

    companion object: KLoggingChannel()

    @Test
    fun `generate simple column condition`() {
        deleteFrom("foo")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifColumn("v").eq(1.literal())
            .asCql() shouldBeEqualTo "DELETE FROM foo WHERE k=? IF v=1"

        deleteFrom("foo")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .if_(
                Condition.column("v1").eq(1.literal()),
                Condition.column("v2").eq(2.literal())
            )
            .asCql() shouldBeEqualTo "DELETE FROM foo WHERE k=? IF v1=1 AND v2=2"
    }

    @Test
    fun `generate field condition`() {
        deleteFrom("foo")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifField("v", "f").eq(1.literal())
            .ifField("v", "g").eq(2.literal())
            .asCql() shouldBeEqualTo "DELETE FROM foo WHERE k=? IF v.f=1 AND v.g=2"
    }

    @Test
    fun `generate element condition`() {
        deleteFrom("foo")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifElement("v", 1.literal()).eq(2.literal())
            .asCql() shouldBeEqualTo "DELETE FROM foo WHERE k=? IF v[1]=2"
    }

    @Test
    fun `generate cancel other conditions if ifExists`() {
        deleteFrom("foo")
            .whereColumn("k").eq(QueryBuilder.bindMarker())
            .ifColumn("v1").eq(1.literal())
            .ifColumn("v2").eq(2.literal())
            .ifExists()
            .asCql() shouldBeEqualTo "DELETE FROM foo WHERE k=? IF EXISTS"
    }
}

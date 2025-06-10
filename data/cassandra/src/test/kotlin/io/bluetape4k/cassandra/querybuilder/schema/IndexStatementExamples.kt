package io.bluetape4k.cassandra.querybuilder.schema

import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createIndex
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropIndex
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class IndexStatementExamples {

    companion object: KLoggingChannel()

    @Test
    fun `generate createIndex`() {
        createIndex().toString() shouldBeEqualTo "CREATE INDEX"
        createIndex().onTable("foo").toString() shouldBeEqualTo "CREATE INDEX ON foo"
        createIndex().ifNotExists().onTable("foo").toString() shouldBeEqualTo "CREATE INDEX IF NOT EXISTS ON foo"

        createIndex()
            .custom("MyClass")
            .ifNotExists()
            .onTable("foo")
            .andColumn("x")
            .toString() shouldBeEqualTo "CREATE CUSTOM INDEX IF NOT EXISTS ON foo (x) USING 'MyClass'"

        createIndex("x")
            .ifNotExists()
            .onTable("ks", "foo")
            .andColumn("y")
            .toString() shouldBeEqualTo "CREATE INDEX IF NOT EXISTS x ON ks.foo (y)"
    }


    @Test
    fun `generate dropIndex`() {
        dropIndex("ks", "idx1")
            .ifExists()
            .asCql() shouldBeEqualTo "DROP INDEX IF EXISTS ks.idx1"
    }
}

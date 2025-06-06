package io.bluetape4k.cassandra.querybuilder.schema

import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.alterMaterializedView
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createMaterializedView
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropMaterializedView
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.cassandra.querybuilder.lt
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MaterializedViewStatementExamples {

    companion object: KLoggingChannel()

    @Test
    fun `generate alterMaterializedView`() {
        alterMaterializedView("ks", "foo")
            .withLZ4Compression()
            .withDefaultTimeToLiveSeconds(86400)
            .asCql() shouldBeEqualTo
                "ALTER MATERIALIZED VIEW ks.foo WITH compression={'class':'LZ4Compressor'} AND default_time_to_live=86400"

        alterMaterializedView("ks", "foo")
            .withCDC(true)
            .asCql() shouldBeEqualTo
                "ALTER MATERIALIZED VIEW ks.foo WITH cdc=true"

        alterMaterializedView("ks", "foo")
            .withCaching(true, SchemaBuilder.RowsPerPartition.ALL)
            .asCql() shouldBeEqualTo
                "ALTER MATERIALIZED VIEW ks.foo WITH caching={'keys':'ALL','rows_per_partition':'ALL'}"
    }

    @Test
    fun `generate createMaterializedView`() {
        createMaterializedView("ks", "foo")
            .ifNotExists()
            .asSelectFrom("ks", "tbl1")
            .all()
            .whereColumn("x").isNotNull
            .withPartitionKey("x")
            .withLZ4Compression()
            .withDefaultTimeToLiveSeconds(86400)
            .asCql() shouldBeEqualTo "CREATE MATERIALIZED VIEW IF NOT EXISTS ks.foo" +
                " AS SELECT * FROM ks.tbl1 WHERE x IS NOT NULL PRIMARY KEY(x)" +
                " WITH compression={'class':'LZ4Compressor'} AND default_time_to_live=86400"

        createMaterializedView("ks", "foo")
            .ifNotExists()
            .asSelectFrom("ks", "tbl1")
            .columns("x", "y")
            .whereColumn("x").lt(5.literal())
            .withPartitionKey("x")
            .asCql() shouldBeEqualTo "CREATE MATERIALIZED VIEW IF NOT EXISTS ks.foo" +
                " AS SELECT x,y FROM ks.tbl1 WHERE x<5 PRIMARY KEY(x)"


        createMaterializedView("ks", "foo")
            .ifNotExists()
            .asSelectFrom("ks", "tbl1")
            .all()
            .whereColumn("x").isNotNull
            .whereColumn("y").isNotNull
            .withPartitionKey("x")
            .withPartitionKey("y")
            .withClusteringColumn("a")
            .withClusteringColumn("b")
            .asCql() shouldBeEqualTo "CREATE MATERIALIZED VIEW IF NOT EXISTS ks.foo" +
                " AS SELECT * FROM ks.tbl1 WHERE x IS NOT NULL AND y IS NOT NULL PRIMARY KEY((x,y),a,b)"
    }

    @Test
    fun `generate dropMaterializedView`() {
        dropMaterializedView("ks", "foo")
            .ifExists()
            .asCql() shouldBeEqualTo "DROP MATERIALIZED VIEW IF EXISTS ks.foo"
    }
}

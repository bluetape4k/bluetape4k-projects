package io.bluetape4k.cassandra.querybuilder.schema

import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.alterTable
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createTable
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropTable
import com.datastax.oss.driver.api.querybuilder.schema.compaction.TimeWindowCompactionStrategy
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TableStatementExamples {

    companion object: KLogging()

    @Test
    fun `generate alterTable`() {
        alterTable("ks", "foo").toString() shouldBeEqualTo "ALTER TABLE ks.foo"

        alterTable("ks", "foo")
            .alterColumn("x", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "ALTER TABLE ks.foo ALTER x TYPE text"

        alterTable("ks", "foo")
            .addColumn("x", DataTypes.TEXT)
            .addStaticColumn("y", DataTypes.FLOAT)
            .addColumn("z", DataTypes.DOUBLE)
            .asCql() shouldBeEqualTo "ALTER TABLE ks.foo ADD (x text,y float STATIC,z double)"

        alterTable("ks", "foo")
            .dropColumns("x", "y")
            .dropColumn("z")
            .asCql() shouldBeEqualTo "ALTER TABLE ks.foo DROP (x,y,z)"

        alterTable("ks", "foo")
            .renameColumn("x", "y")
            .renameColumn("u", "v")
            .asCql() shouldBeEqualTo "ALTER TABLE ks.foo RENAME x TO y AND u TO v"

        alterTable("ks", "foo")
            .dropCompactStorage()
            .asCql() shouldBeEqualTo "ALTER TABLE ks.foo DROP COMPACT STORAGE"

        alterTable("ks", "foo")
            .withLZ4Compression()
            .asCql() shouldBeEqualTo "ALTER TABLE ks.foo WITH compression={'class':'LZ4Compressor'}"

        alterTable("ks", "foo")
            .withNoCompression()
            .asCql() shouldBeEqualTo "ALTER TABLE ks.foo WITH compression={'sstable_compression':''}"
    }


    @Test
    fun `generate createTable`() {
        createTable("ks", "foo")
            .ifNotExists()
            .withPartitionKey("k", DataTypes.INT)
            .asCql() shouldBeEqualTo "CREATE TABLE IF NOT EXISTS ks.foo (k int PRIMARY KEY)"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text)"

        createTable("foo")
            .withPartitionKey("kc", DataTypes.INT)
            .withPartitionKey("ka", DataTypes.TIMESTAMP)
            .withColumn("v", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "CREATE TABLE foo (kc int,ka timestamp,v text,PRIMARY KEY((kc,ka)))"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withClusteringColumn("c", DataTypes.TEXT)
            .withStaticColumn("s", DataTypes.TIMEUUID)
            .withColumn("v", SchemaBuilder.udt("value", true))
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int,c text,s timeuuid STATIC,v frozen<value>,PRIMARY KEY(k,c))"

        createTable("foo")
            .withPartitionKey("kc", DataTypes.INT)
            .withPartitionKey("ka", DataTypes.TIMESTAMP)
            .withClusteringColumn("c", DataTypes.FLOAT)
            .withClusteringColumn("a", DataTypes.UUID)
            .withColumn("v", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "CREATE TABLE foo (kc int,ka timestamp,c float,a uuid,v text,PRIMARY KEY((kc,ka),c,a))"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withCompactStorage()
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) WITH COMPACT STORAGE"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withClusteringColumn("c", DataTypes.TEXT)
            .withColumn("v", DataTypes.TEXT)
            .withClusteringOrder("c", ClusteringOrder.ASC)
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int,c text,v text,PRIMARY KEY(k,c)) " +
                "WITH CLUSTERING ORDER BY (c ASC)"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withClusteringColumn("c0", DataTypes.TEXT)
            .withClusteringColumn("c1", DataTypes.TEXT)
            .withClusteringColumn("c2", DataTypes.TEXT)
            .withColumn("v", DataTypes.TEXT)
            .withClusteringOrder("c0", ClusteringOrder.DESC)
            .withClusteringOrder(mapOf("c1" to ClusteringOrder.ASC, "c2" to ClusteringOrder.DESC))
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int,c0 text,c1 text,c2 text,v text,PRIMARY KEY(k,c0,c1,c2)) " +
                "WITH CLUSTERING ORDER BY (c0 DESC,c1 ASC,c2 DESC)"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withCompactStorage()
            .withDefaultTimeToLiveSeconds(86400)
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) " +
                "WITH COMPACT STORAGE AND default_time_to_live=86400"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withPartitionKey("m", DataTypes.TEXT)
            .withClusteringColumn("c", DataTypes.TEXT)
            .withColumn("v", DataTypes.TEXT)
            .withCompactStorage()
            .withClusteringOrder("c", ClusteringOrder.DESC)
            .withDefaultTimeToLiveSeconds(86400)
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int,m text,c text,v text,PRIMARY KEY((k,m),c)) " +
                "WITH COMPACT STORAGE AND CLUSTERING ORDER BY (c DESC) AND default_time_to_live=86400"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withBloomFilterFpChance(0.42)
            .withCDC(false)
            .withComment("Hello world")
            .withDcLocalReadRepairChance(0.54)
            .withDefaultTimeToLiveSeconds(86400)
            .withGcGraceSeconds(864000)
            .withMemtableFlushPeriodInMs(10000)
            .withMinIndexInterval(1024)
            .withMaxIndexInterval(4096)
            .withReadRepairChance(0.55)
            .withSpeculativeRetry("99percentile")
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) " +
                "WITH bloom_filter_fp_chance=0.42 AND cdc=false AND comment='Hello world' " +
                "AND dclocal_read_repair_chance=0.54 AND default_time_to_live=86400 AND gc_grace_seconds=864000 " +
                "AND memtable_flush_period_in_ms=10000 AND min_index_interval=1024 AND max_index_interval=4096 " +
                "AND read_repair_chance=0.55 AND speculative_retry='99percentile'"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withLZ4Compression(1024, 0.5)
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) " +
                "WITH compression={'class':'LZ4Compressor','chunk_length_kb':1024,'crc_check_chance':0.5}"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withCaching(true, SchemaBuilder.RowsPerPartition.rows(10))
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) " +
                "WITH caching={'keys':'ALL','rows_per_partition':'10'}"
    }

    @Test
    fun `generate createTable withCompaction`() {
        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withCompaction(
                SchemaBuilder.sizeTieredCompactionStrategy()
                    .withBucketHigh(1.6)
                    .withBucketLow(0.6)
                    .withColdReadsToOmit(0.1)
                    .withMaxThreshold(33)
                    .withMinThreshold(5)
                    .withMinSSTableSizeInBytes(50000)
                    .withOnlyPurgeRepairedTombstones(true)
                    .withEnabled(false)
                    .withTombstoneCompactionIntervalInSeconds(86400)
                    .withTombstoneThreshold(0.22)
                    .withUncheckedTombstoneCompaction(true)
            )
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) WITH compaction={" +
                "'class':'SizeTieredCompactionStrategy','bucket_high':1.6,'bucket_low':0.6,'cold_reads_to_omit':0.1," +
                "'max_threshold':33,'min_threshold':5,'min_sstable_size':50000,'only_purge_repaired_tombstones':true," +
                "'enabled':false,'tombstone_compaction_interval':86400,'tombstone_threshold':0.22,'unchecked_tombstone_compaction':true}"


        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withCompaction(
                SchemaBuilder.leveledCompactionStrategy()
                    .withSSTableSizeInMB(110)
                    .withTombstoneCompactionIntervalInSeconds(3600)
            )
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) " +
                "WITH compaction={'class':'LeveledCompactionStrategy','sstable_size_in_mb':110,'tombstone_compaction_interval':3600}"

        createTable("foo")
            .withPartitionKey("k", DataTypes.INT)
            .withColumn("v", DataTypes.TEXT)
            .withCompaction(
                SchemaBuilder.timeWindowCompactionStrategy()
                    .withCompactionWindow(10, TimeWindowCompactionStrategy.CompactionWindowUnit.DAYS)
                    .withTimestampResolution(TimeWindowCompactionStrategy.TimestampResolution.MICROSECONDS)
                    .withUnsafeAggressiveSSTableExpiration(false)
            )
            .asCql() shouldBeEqualTo "CREATE TABLE foo (k int PRIMARY KEY,v text) " +
                "WITH compaction={'class':'TimeWindowCompactionStrategy','compaction_window_size':10," +
                "'compaction_window_unit':'DAYS','timestamp_resolution':'MICROSECONDS','unsafe_aggressive_sstable_expiration':false}"
    }

    @Test
    fun `generate dropTable`() {
        dropTable("ks", "foo")
            .ifExists()
            .asCql() shouldBeEqualTo "DROP TABLE IF EXISTS ks.foo"
    }
}

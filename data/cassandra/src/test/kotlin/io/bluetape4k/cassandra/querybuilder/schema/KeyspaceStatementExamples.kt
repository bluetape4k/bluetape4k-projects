package io.bluetape4k.cassandra.querybuilder.schema

import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.alterKeyspace
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createKeyspace
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropKeyspace
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class KeyspaceStatementExamples {

    companion object: KLoggingChannel()

    @Test
    fun `generate alterKeyspace`() {
        alterKeyspace("ks")
            .withSimpleStrategy(3)
            .asCql() shouldBeEqualTo
                "ALTER KEYSPACE ks WITH replication={'class':'SimpleStrategy','replication_factor':3}"

        alterKeyspace("ks")
            .withDurableWrites(true)
            .withOption("hello", "world")
            .asCql() shouldBeEqualTo
                "ALTER KEYSPACE ks WITH durable_writes=true AND hello='world'"
    }

    @Test
    fun `generate createKeyspace`() {
        createKeyspace("ks")
            .ifNotExists()
            .withSimpleStrategy(3)
            .asCql() shouldBeEqualTo
                "CREATE KEYSPACE IF NOT EXISTS ks WITH replication={'class':'SimpleStrategy','replication_factor':3}"

        createKeyspace("ks")
            .ifNotExists()
            .withSimpleStrategy(3)
            .withDurableWrites(true)
            .asCql() shouldBeEqualTo
                "CREATE KEYSPACE IF NOT EXISTS ks WITH replication={'class':'SimpleStrategy','replication_factor':3} AND durable_writes=true"

        createKeyspace("ks")
            .ifNotExists()
            .withNetworkTopologyStrategy(mapOf("dc1" to 3, "dc2" to 4))
            .asCql() shouldBeEqualTo
                "CREATE KEYSPACE IF NOT EXISTS ks WITH replication={'class':'NetworkTopologyStrategy','dc1':3,'dc2':4}"

        createKeyspace("ks")
            .ifNotExists()
            .withSimpleStrategy(3)
            .withOption("awesome_feature", true)
            .withOption("wow_factor", 11)
            .asCql() shouldBeEqualTo
                "CREATE KEYSPACE IF NOT EXISTS ks WITH replication={'class':'SimpleStrategy','replication_factor':3} AND awesome_feature=true AND wow_factor=11"
    }

    @Test
    fun `generate dropKeyspace`() {
        dropKeyspace("ks")
            .ifExists()
            .asCql() shouldBeEqualTo "DROP KEYSPACE IF EXISTS ks"
    }
}

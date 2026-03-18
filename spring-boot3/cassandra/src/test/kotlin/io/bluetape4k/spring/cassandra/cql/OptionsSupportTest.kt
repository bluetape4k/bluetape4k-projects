package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class OptionsSupportTest {

    companion object {
        private const val KEYSPACE = "ks"
        private const val TABLE = "tbl"
        private const val TIMESTAMP = 1700000000000L
    }

    @Test
    fun `writeOptions should detect positive ttl`() {
        writeOptions { ttl(Duration.ofSeconds(10)) }.isPositiveTtl.shouldBeTrue()
        writeOptions { ttl(Duration.ZERO) }.isPositiveTtl.shouldBeTrue()
    }

    @Test
    fun `insert should apply ttl`() {
        val insert = QueryBuilder.insertInto(KEYSPACE, TABLE)
            .value("id", QueryBuilder.literal(1))
            .value("name", QueryBuilder.literal("a"))

        val options = writeOptions {
            ttl(Duration.ofSeconds(3))
        }

        val expected = insert
            .usingTtl(3)
            .build()
            .query

        val actual = insert.addWriteOptions(options).build().query

        actual shouldBeEqualTo expected
    }

    @Test
    fun `update should apply timestamp`() {
        val updateStart = QueryBuilder.update(KEYSPACE, TABLE)

        val options = writeOptions {
            timestamp(TIMESTAMP)
        }

        val expected = updateStart
            .usingTimestamp(TIMESTAMP)
            .setColumn("name", QueryBuilder.literal("b"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query

        val actual = updateStart
            .addWriteOptions(options)
            .setColumn("name", QueryBuilder.literal("b"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query

        actual shouldBeEqualTo expected
    }

    @Test
    fun `delete should apply timestamp`() {
        val delete = QueryBuilder.deleteFrom(KEYSPACE, TABLE)
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))

        val options = writeOptions {
            timestamp(TIMESTAMP)
        }

        val cql = delete.addWriteOptions(options).build().query

        cql.contains("USING TIMESTAMP $TIMESTAMP").shouldBeEqualTo(true)
    }
}

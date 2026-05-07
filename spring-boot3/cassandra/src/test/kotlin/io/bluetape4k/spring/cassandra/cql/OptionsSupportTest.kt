package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration

class OptionsSupportTest {

    companion object: KLogging() {
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

    @Test
    fun `update addWriteOptions - UpdateStart에 TTL과 timestamp를 함께 적용한다`() {
        val updateStart = QueryBuilder.update(KEYSPACE, TABLE)

        val options = writeOptions {
            ttl(Duration.ofSeconds(60))
            timestamp(TIMESTAMP)
        }

        val expected = updateStart
            .usingTtl(60)
            .usingTimestamp(TIMESTAMP)
            .setColumn("name", QueryBuilder.literal("c"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query

        val actual = updateStart
            .addWriteOptions(options)
            .setColumn("name", QueryBuilder.literal("c"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query

        actual shouldBeEqualTo expected
    }

    @Test
    fun `update addWriteOptions - assignment 이후에도 옵션을 적용한다`() {
        val updateWithAssignment = QueryBuilder.update(KEYSPACE, TABLE)
            .setColumn("name", QueryBuilder.literal("d"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))

        val options = writeOptions {
            ttl(Duration.ofSeconds(30))
            timestamp(TIMESTAMP)
        }

        // DataStax 구현체는 항상 UpdateStart를 구현하므로 옵션이 적용됨
        val actual = updateWithAssignment.addWriteOptions(options).build().query

        actual.contains("USING TIMESTAMP $TIMESTAMP") shouldBeEqualTo true
        actual.contains("TTL 30") shouldBeEqualTo true
    }

    @Test
    fun `insert addWriteOptions - TTL과 timestamp를 함께 적용한다`() {
        val insert = QueryBuilder.insertInto(KEYSPACE, TABLE)
            .value("id", QueryBuilder.literal(1))
            .value("name", QueryBuilder.literal("e"))

        val options = writeOptions {
            ttl(Duration.ofSeconds(120))
            timestamp(TIMESTAMP)
        }

        val expected = insert
            .usingTtl(120)
            .usingTimestamp(TIMESTAMP)
            .build()
            .query

        val actual = insert.addWriteOptions(options).build().query

        actual shouldBeEqualTo expected
    }

    @Test
    fun `writeOptions - ttl이 null이면 isPositiveTtl이 false`() {
        val options = writeOptions { }
        options.isPositiveTtl shouldBeEqualTo false
    }

    @Test
    fun `deleteOptions - 빌더 DSL로 생성한다`() {
        val options = deleteOptions { }
        options.shouldNotBeNull()
    }

    @Test
    fun `queryOptions - 빌더 DSL로 생성한다`() {
        val options = queryOptions { }
        options.shouldNotBeNull()
    }

    @Test
    fun `insertOptions - 빌더 DSL로 생성한다`() {
        val options = insertOptions { }
        options.shouldNotBeNull()
    }

    @Test
    fun `updateOptions - 빌더 DSL로 생성한다`() {
        val options = updateOptions { }
        options.shouldNotBeNull()
    }
}

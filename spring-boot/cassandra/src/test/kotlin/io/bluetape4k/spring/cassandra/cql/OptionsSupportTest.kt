package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.update.Update
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.mockk.mockk
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
    fun `writeOptions should reject negative ttl with Spring Data message`() {
        val error = assertFailsWith<IllegalArgumentException> {
            writeOptions { ttl(Duration.ofSeconds(-1)) }
        }

        error.message shouldBeEqualTo "TTL must be greater than equal to zero"
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
    fun `insert should render zero ttl for zero and subsecond durations`() {
        val insert = QueryBuilder.insertInto(KEYSPACE, TABLE)
            .value("id", QueryBuilder.literal(1))

        listOf(Duration.ZERO, Duration.ofMillis(1), Duration.ofMillis(500)).forEach { ttl ->
            val actual = insert
                .addWriteOptions(writeOptions { ttl(ttl) })
                .build()
                .query
            val expected = insert.usingTtl(0).build().query

            actual shouldBeEqualTo expected
        }
    }

    @Test
    fun `insert should reject ttl seconds outside int range`() {
        val insert = QueryBuilder.insertInto(KEYSPACE, TABLE)
            .value("id", QueryBuilder.literal(1))
        val options = writeOptions { ttl(Duration.ofSeconds(Int.MAX_VALUE.toLong() + 1)) }

        assertFailsWith<ArithmeticException> {
            insert.addWriteOptions(options)
        }
    }

    @Test
    fun `insert without ttl should not render a ttl clause`() {
        val insert = QueryBuilder.insertInto(KEYSPACE, TABLE)
            .value("id", QueryBuilder.literal(1))

        val cql = insert.addWriteOptions(writeOptions { }).build().query

        cql.contains("USING TTL").shouldBeFalse()
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
    fun `update start should apply zero ttl and timestamp`() {
        val updateStart = QueryBuilder.update(KEYSPACE, TABLE)
        val options = writeOptions {
            ttl(Duration.ZERO)
            timestamp(TIMESTAMP)
        }

        val actual = updateStart
            .addWriteOptions(options)
            .setColumn("name", QueryBuilder.literal("b"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query
        val expected = updateStart
            .usingTtl(0)
            .usingTimestamp(TIMESTAMP)
            .setColumn("name", QueryBuilder.literal("b"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query

        actual shouldBeEqualTo expected
    }

    @Test
    fun `update start should apply positive ttl`() {
        val updateStart = QueryBuilder.update(KEYSPACE, TABLE)
        val options = writeOptions { ttl(Duration.ofSeconds(5)) }

        val actual = updateStart
            .addWriteOptions(options)
            .setColumn("name", QueryBuilder.literal("b"))
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query
        val expected = updateStart
            .usingTtl(5)
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

        val actual = delete.addWriteOptions(options).build().query
        val expected = QueryBuilder.deleteFrom(KEYSPACE, TABLE)
            .usingTimestamp(TIMESTAMP)
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query

        actual shouldBeEqualTo expected
    }

    @Test
    fun `delete should preserve timestamp without applying ttl`() {
        val delete = QueryBuilder.deleteFrom(KEYSPACE, TABLE)
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
        val options = writeOptions {
            ttl(Duration.ofSeconds(5))
            timestamp(TIMESTAMP)
        }

        val actual = delete.addWriteOptions(options).build().query
        val expected = QueryBuilder.deleteFrom(KEYSPACE, TABLE)
            .usingTimestamp(TIMESTAMP)
            .whereColumn("id").isEqualTo(QueryBuilder.literal(1))
            .build()
            .query

        actual shouldBeEqualTo expected
        actual.contains("USING TTL").shouldBeFalse()
    }

    @Test
    fun `queryOptions builder should create instance`() {
        val options = queryOptions { pageSize(100) }
        options.shouldNotBeNull()
    }

    @Test
    fun `insertOptions builder should create instance`() {
        val options = insertOptions { withIfNotExists() }
        options.isIfNotExists.shouldBeTrue()
    }

    @Test
    fun `updateOptions builder should create instance`() {
        val options = updateOptions { withIfExists() }
        options.isIfExists.shouldBeTrue()
    }

    @Test
    fun `deleteOptions builder should create instance`() {
        val options = deleteOptions { }
        options.shouldNotBeNull()
    }

    @Test
    fun `writeOptions with no ttl should not be positive ttl`() {
        val options = writeOptions { }
        options.isPositiveTtl.shouldBeFalse()
    }

    @Test
    fun `insert should apply both ttl and timestamp`() {
        val insert = QueryBuilder.insertInto(KEYSPACE, TABLE)
            .value("id", QueryBuilder.literal(1))

        val options = writeOptions {
            ttl(Duration.ofSeconds(5))
            timestamp(TIMESTAMP)
        }

        val actual = insert.addWriteOptions(options).build().query
        val expected = insert
            .usingTtl(5)
            .usingTimestamp(TIMESTAMP)
            .build()
            .query

        actual shouldBeEqualTo expected
    }

    @Test
    fun `update non-UpdateStart should not apply options`() {
        val update = mockk<Update>()
        val options = writeOptions { timestamp(TIMESTAMP) }

        val result = update.addWriteOptions(options)
        (result === update).shouldBeTrue()
    }
}

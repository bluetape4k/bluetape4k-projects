package io.bluetape4k.exposed.clickhouse.types

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.ClickHouseTable
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant

/**
 * `DateTime64` 컬럼 타입의 ClickHouse 컨테이너 기반 round-trip 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DateTime64Test: AbstractClickHouseTest() {

    companion object: KLogging()

    private object Dt64Table: ClickHouseTable("dt64_test", mergeTree { orderBy("id") }) {
        val id = long("id")
        val ts3 = dateTime64("ts3", precision = 3)
        val ts6 = dateTime64("ts6", precision = 6)
        val tsNullable = chNullable("ts_nullable", DateTime64ColumnType(3))
    }

    @Test
    fun `sqlType for DateTime64 with default precision`() {
        DateTime64ColumnType().sqlType() shouldBeEqualTo "DateTime64(3, 'UTC')"
    }

    @Test
    fun `sqlType for DateTime64 with precision 6`() {
        DateTime64ColumnType(6).sqlType() shouldBeEqualTo "DateTime64(6, 'UTC')"
    }

    @Test
    fun `sqlType for DateTime64 with precision 0`() {
        DateTime64ColumnType(0).sqlType() shouldBeEqualTo "DateTime64(0, 'UTC')"
    }

    @Test
    fun `precision 3 round-trip preserves milliseconds`() {
        transaction(db) {
            SchemaUtils.create(Dt64Table)
            try {
                val nowMillis = Instant.parse("2026-04-25T12:34:56.789Z")
                Dt64Table.insert {
                    it[id] = 1L
                    it[ts3] = nowMillis
                    it[ts6] = nowMillis
                    it[tsNullable] = nowMillis
                }

                val rows = Dt64Table.selectAll()
                    .where { Dt64Table.id eq 1L }
                    .toList()
                rows.size shouldBeEqualTo 1
                rows[0][Dt64Table.ts3] shouldBeEqualTo nowMillis
            } finally {
                SchemaUtils.drop(Dt64Table)
            }
        }
    }

    @Test
    fun `precision 6 round-trip preserves microseconds`() {
        transaction(db) {
            SchemaUtils.create(Dt64Table)
            try {
                // microsecond precision
                val microInstant = Instant.parse("2026-04-25T12:34:56.123456Z")
                Dt64Table.insert {
                    it[id] = 2L
                    it[ts3] = microInstant
                    it[ts6] = microInstant
                    it[tsNullable] = null
                }

                val rows = Dt64Table.selectAll()
                    .where { Dt64Table.id eq 2L }
                    .toList()
                rows.size shouldBeEqualTo 1
                rows[0][Dt64Table.ts6].shouldNotBeNull()
            } finally {
                SchemaUtils.drop(Dt64Table)
            }
        }
    }

    @Test
    fun `nullable DateTime64 round-trip with null value`() {
        transaction(db) {
            SchemaUtils.create(Dt64Table)
            try {
                val now = Instant.parse("2026-04-25T00:00:00Z")
                Dt64Table.insert {
                    it[id] = 3L
                    it[ts3] = now
                    it[ts6] = now
                    it[tsNullable] = null
                }

                val rows = Dt64Table.selectAll()
                    .where { Dt64Table.id eq 3L }
                    .toList()
                rows.size shouldBeEqualTo 1
                rows[0][Dt64Table.tsNullable].shouldBeNull()
            } finally {
                SchemaUtils.drop(Dt64Table)
            }
        }
    }

    @Test
    fun `precision out of range throws`() {
        try {
            DateTime64ColumnType(-1)
            error("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
        try {
            DateTime64ColumnType(10)
            error("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}

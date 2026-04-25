package io.bluetape4k.exposed.clickhouse.types

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.ClickHouseTable
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

/**
 * `Date32` 컬럼 타입의 ClickHouse 컨테이너 기반 round-trip 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Date32Test: AbstractClickHouseTest() {

    companion object: KLogging()

    private object D32Table: ClickHouseTable("d32_test", mergeTree { orderBy("id") }) {
        val id = long("id")
        val date = date32("event_date")
    }

    @Test
    fun `Date32 sqlType is Date32`() {
        Date32ColumnType().sqlType() shouldBeEqualTo "Date32"
    }

    @Test
    fun `createStatement contains Date32`() {
        val ddl = transaction(db) { D32Table.createStatement().joinToString("\n") }
        ddl shouldContain "Date32"
    }

    @Test
    fun `LocalDate round-trip with today`() {
        transaction(db) {
            SchemaUtils.create(D32Table)
            try {
                val today = LocalDate.now()
                D32Table.insert {
                    it[id] = 1L
                    it[date] = today
                }
                val rows = D32Table.selectAll()
                    .where { D32Table.id eq 1L }
                    .toList()
                rows.size shouldBeEqualTo 1
                rows[0][D32Table.date] shouldBeEqualTo today
            } finally {
                SchemaUtils.drop(D32Table)
            }
        }
    }

    @Test
    fun `LocalDate round-trip with boundary dates`() {
        transaction(db) {
            SchemaUtils.create(D32Table)
            try {
                // Date32 supports 1900-01-01 to 2299-12-31
                val minDate = LocalDate.of(1925, 1, 1)
                val maxDate = LocalDate.of(2283, 11, 11)

                D32Table.insert {
                    it[id] = 2L
                    it[date] = minDate
                }
                D32Table.insert {
                    it[id] = 3L
                    it[date] = maxDate
                }

                val rowMin = D32Table.selectAll()
                    .where { D32Table.id eq 2L }
                    .single()
                rowMin[D32Table.date] shouldBeEqualTo minDate

                val rowMax = D32Table.selectAll()
                    .where { D32Table.id eq 3L }
                    .single()
                rowMax[D32Table.date] shouldBeEqualTo maxDate
            } finally {
                SchemaUtils.drop(D32Table)
            }
        }
    }
}

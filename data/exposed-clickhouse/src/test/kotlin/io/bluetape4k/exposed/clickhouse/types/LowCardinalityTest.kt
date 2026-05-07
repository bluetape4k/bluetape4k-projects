package io.bluetape4k.exposed.clickhouse.types

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.ClickHouseTable
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContain
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * `LowCardinality(String)` 컬럼 타입의 ClickHouse 컨테이너 기반 round-trip 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LowCardinalityTest: AbstractClickHouseTest() {

    companion object: KLogging()

    private object LcTable: ClickHouseTable("lc_test", mergeTree { orderBy("id") }) {
        val id = long("id")
        val category = lowCardinalityString("category")
        val nullableCategory = chNullable("nullable_category", ClickHouseStringColumnType())
    }

    @Test
    fun `LowCardinality(String) sqlType`() {
        LowCardinalityColumnType(ClickHouseStringColumnType()).sqlType() shouldBeEqualTo "LowCardinality(String)"
    }

    @Test
    fun `LowCardinality(FixedString) sqlType`() {
        LowCardinalityColumnType(ClickHouseFixedStringColumnType(8)).sqlType() shouldBeEqualTo
            "LowCardinality(FixedString(8))"
    }

    @Test
    fun `createStatement DDL contains LowCardinality(String)`() {
        val ddl = transaction(db) { LcTable.createStatement().joinToString("\n") }
        ddl shouldContain "LowCardinality(String)"
    }

    @Test
    fun `lowCardinalityString round-trip with multiple values`() {
        transaction(db) {
            SchemaUtils.create(LcTable)
            try {
                val data = listOf(
                    Triple(1L, "ALPHA", "a-1"),
                    Triple(2L, "BETA", null),
                    Triple(3L, "ALPHA", "a-3"),
                    Triple(4L, "GAMMA", "g-4"),
                )

                LcTable.batchInsert(data) { (theId, cat, nullCat) ->
                    this[LcTable.id] = theId
                    this[LcTable.category] = cat
                    this[LcTable.nullableCategory] = nullCat
                }

                val rows = LcTable.selectAll().orderBy(LcTable.id).toList()
                rows.size shouldBeEqualTo 4
                rows[0][LcTable.category] shouldBeEqualTo "ALPHA"
                rows[1][LcTable.category] shouldBeEqualTo "BETA"
                rows[1][LcTable.nullableCategory].shouldBeNull()
                rows[2][LcTable.category] shouldBeEqualTo "ALPHA"
                rows[3][LcTable.category] shouldBeEqualTo "GAMMA"
            } finally {
                SchemaUtils.drop(LcTable)
            }
        }
    }
}

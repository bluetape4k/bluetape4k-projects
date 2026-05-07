package io.bluetape4k.exposed.clickhouse.types

import io.bluetape4k.exposed.clickhouse.AbstractClickHouseTest
import io.bluetape4k.exposed.clickhouse.ClickHouseTable
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * `Array(T)` 컬럼 타입의 ClickHouse 컨테이너 기반 round-trip 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArrayTypeTest: AbstractClickHouseTest() {

    companion object: KLogging()

    private object ArrTable: ClickHouseTable("arr_test", mergeTree { orderBy("id") }) {
        val id = long("id")
        val tags = chArray("tags", ClickHouseStringColumnType())
        val nums = chArray("nums", ClickHouseInt32ColumnType())
    }

    @Test
    fun `Array(String) sqlType`() {
        ClickHouseArrayColumnType(ClickHouseStringColumnType()).sqlType() shouldBeEqualTo "Array(String)"
    }

    @Test
    fun `Array(Int32) sqlType`() {
        ClickHouseArrayColumnType(ClickHouseInt32ColumnType()).sqlType() shouldBeEqualTo "Array(Int32)"
    }

    @Test
    fun `createStatement contains Array(String) and Array(Int32)`() {
        val ddl = transaction(db) { ArrTable.createStatement().joinToString("\n") }
        ddl shouldContain "Array(String)"
        ddl shouldContain "Array(Int32)"
    }

    @Test
    fun `Array(String) round-trip with various sizes`() {
        transaction(db) {
            SchemaUtils.create(ArrTable)
            try {
                ArrTable.insert {
                    it[id] = 1L
                    it[tags] = emptyList()
                    it[nums] = emptyList()
                }
                ArrTable.insert {
                    it[id] = 2L
                    it[tags] = listOf("single")
                    it[nums] = listOf(42)
                }
                ArrTable.insert {
                    it[id] = 3L
                    it[tags] = listOf("alpha", "beta", "gamma")
                    it[nums] = listOf(-1, 0, 1, 100)
                }

                val rows = ArrTable.selectAll().orderBy(ArrTable.id).toList()
                rows.size shouldBeEqualTo 3
                rows[0][ArrTable.tags] shouldBeEqualTo emptyList()
                rows[0][ArrTable.nums] shouldBeEqualTo emptyList()
                rows[1][ArrTable.tags] shouldBeEqualTo listOf("single")
                rows[1][ArrTable.nums] shouldBeEqualTo listOf(42)
                rows[2][ArrTable.tags] shouldBeEqualTo listOf("alpha", "beta", "gamma")
                rows[2][ArrTable.nums] shouldBeEqualTo listOf(-1, 0, 1, 100)
            } finally {
                SchemaUtils.drop(ArrTable)
            }
        }
    }
}

package io.bluetape4k.spring.batch.exposed.partition

import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.exposed.tests.TestDB
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ExposedRangePartitionerTest : AbstractExposedBatchTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 테이블에서 단일 빈 파티션 반환`(testDB: TestDB) {
        withBatchTables(testDB) {
            val partitioner = ExposedRangePartitioner.forEntityId(
                table = SourceTable,
                gridSize = 4,
                database = testDB.db,
            )

            val partitions = partitioner.partition(4)

            partitions.size shouldBeEqualTo 1
            partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MIN_ID) shouldBeEqualTo 0L
            partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MAX_ID) shouldBeEqualTo -1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `1000건 데이터를 4개 파티션으로 균등 분할`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(1000)

            val partitioner = ExposedRangePartitioner.forEntityId(
                table = SourceTable,
                gridSize = 4,
                database = testDB.db,
            )

            val partitions = partitioner.partition(4)

            partitions.size shouldBeEqualTo 4

            val ranges = partitions.values.map { ctx ->
                ctx.getLong(ExposedRangePartitioner.PARTITION_MIN_ID)..ctx.getLong(ExposedRangePartitioner.PARTITION_MAX_ID)
            }.sortedBy { it.first }

            ranges.first().first shouldBeEqualTo 1L
            ranges.last().last shouldBeEqualTo 1000L

            for (i in 0 until ranges.size - 1) {
                ranges[i].last + 1 shouldBeEqualTo ranges[i + 1].first
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `단일 행 테이블에서 1개 파티션 반환`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(1)

            val partitioner = ExposedRangePartitioner.forEntityId(
                table = SourceTable,
                gridSize = 8,
                database = testDB.db,
            )

            val partitions = partitioner.partition(8)

            partitions.size shouldBeEqualTo 1
            partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MIN_ID) shouldBeEqualTo 1L
            partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MAX_ID) shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `gridSize가 totalRange보다 클 때 safeGridSize로 보정`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(3)

            val partitioner = ExposedRangePartitioner.forEntityId(
                table = SourceTable,
                gridSize = 10,
                database = testDB.db,
            )

            val partitions = partitioner.partition(10)

            partitions.size shouldBeLessOrEqualTo 3
            partitions.size shouldBeGreaterOrEqualTo 1
        }
    }
}

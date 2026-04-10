package io.bluetape4k.spring.batch.exposed.reader

import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.batch.infrastructure.item.ExecutionContext

class ExposedKeysetItemReaderTest : AbstractExposedBatchTest() {

    private fun createReader(testDB: TestDB): ExposedKeysetItemReader<SourceRecord> =
        ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 10,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = testDB.db,
        )

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `정상적으로 모든 레코드를 keyset 페이징으로 읽기`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(25)

            val reader = createReader(testDB)
            val context = ExecutionContext().apply {
                putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 1L)
                putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 25L)
            }
            reader.open(context)

            val results = mutableListOf<SourceRecord>()
            var item = reader.read()
            while (item != null) {
                results.add(item)
                item = reader.read()
            }

            results.size shouldBeEqualTo 25
            results.first().id shouldBeEqualTo 1L
            results.last().id shouldBeEqualTo 25L

            reader.close()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 파티션에서 즉시 null 반환`(testDB: TestDB) {
        withBatchTables(testDB) {
            val reader = createReader(testDB)
            val context = ExecutionContext().apply {
                putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 1L)
                putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 100L)
            }
            reader.open(context)
            reader.read().shouldBeNull()
            reader.close()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `restart 시 lastKey부터 이어서 읽기`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(50)

            val reader = createReader(testDB)
            val context = ExecutionContext().apply {
                putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 1L)
                putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 50L)
            }
            reader.open(context)

            repeat(15) { reader.read().shouldNotBeNull() }
            reader.update(context)
            reader.close()

            val restartReader = createReader(testDB)
            restartReader.open(context)

            val remaining = mutableListOf<SourceRecord>()
            var item = restartReader.read()
            while (item != null) {
                remaining.add(item)
                item = restartReader.read()
            }

            remaining.size shouldBeEqualTo 35
            remaining.first().id shouldBeEqualTo 16L

            restartReader.close()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `파티션 범위 내 데이터만 읽기`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(100)

            val reader = createReader(testDB)
            val context = ExecutionContext().apply {
                putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 21L)
                putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 40L)
            }
            reader.open(context)

            val results = mutableListOf<SourceRecord>()
            var item = reader.read()
            while (item != null) {
                results.add(item)
                item = reader.read()
            }

            results.size shouldBeEqualTo 20
            results.first().id shouldBeEqualTo 21L
            results.last().id shouldBeEqualTo 40L

            reader.close()
        }
    }
}

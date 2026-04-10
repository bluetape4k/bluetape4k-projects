package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.batch.infrastructure.item.Chunk

class ExposedItemWriterTest : AbstractExposedBatchTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchInsert로 정상적으로 레코드 삽입`(testDB: TestDB) {
        withBatchTables(testDB) {
            val writer = ExposedItemWriter<TargetRecord>(table = TargetTable) {
                this[TargetTable.sourceName] = it.sourceName
                this[TargetTable.transformedValue] = it.transformedValue
            }

            val items = (1..10).map { TargetRecord("name-$it", it * 2) }
            writer.write(Chunk(items))

            TargetTable.selectAll().count() shouldBeEqualTo 10L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 chunk 전달 시 아무 동작 안 함`(testDB: TestDB) {
        withBatchTables(testDB) {
            val writer = ExposedItemWriter<TargetRecord>(table = TargetTable) {
                this[TargetTable.sourceName] = it.sourceName
                this[TargetTable.transformedValue] = it.transformedValue
            }

            writer.write(Chunk(emptyList()))

            TargetTable.selectAll().count() shouldBeEqualTo 0L
        }
    }
}

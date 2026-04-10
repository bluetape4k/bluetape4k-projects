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

class ExposedUpsertItemWriterTest : AbstractExposedBatchTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert로 신규 insert 및 기존 update 동작 검증`(testDB: TestDB) {
        withBatchTables(testDB) {
            val writer = ExposedUpsertItemWriter<TargetRecord>(table = TargetTable) {
                this[TargetTable.sourceName] = it.sourceName
                this[TargetTable.transformedValue] = it.transformedValue
            }

            val items = (1..5).map { TargetRecord("name-$it", it * 2) }
            writer.write(Chunk(items))
            TargetTable.selectAll().count() shouldBeEqualTo 5L

            val updatedItems = (1..5).map { TargetRecord("name-$it", it * 100) }
            writer.write(Chunk(updatedItems))
            TargetTable.selectAll().count() shouldBeEqualTo 5L

            val updatedValues = TargetTable.selectAll()
                .orderBy(TargetTable.sourceName)
                .map { it[TargetTable.transformedValue] }
            updatedValues shouldBeEqualTo (1..5).map { it * 100 }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 chunk 전달 시 아무 동작 안 함`(testDB: TestDB) {
        withBatchTables(testDB) {
            val writer = ExposedUpsertItemWriter<TargetRecord>(table = TargetTable) {
                this[TargetTable.sourceName] = it.sourceName
                this[TargetTable.transformedValue] = it.transformedValue
            }

            writer.write(Chunk(emptyList()))

            TargetTable.selectAll().count() shouldBeEqualTo 0L
        }
    }
}

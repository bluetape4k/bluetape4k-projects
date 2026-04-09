package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.support.castToLong
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.batch.item.Chunk

class ExposedUpdateItemWriterTest : AbstractExposedBatchTest() {

    private val keyColumn = SourceTable.id.castToLong()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `개별 UPDATE로 레코드 정확히 수정`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(5)

            val writer = ExposedUpdateItemWriter<SourceRecord>(
                table = SourceTable,
                keyColumn = keyColumn,
                keyExtractor = { it.id },
            ) {
                this[SourceTable.name] = it.name + "-updated"
                this[SourceTable.value] = it.value * 10
            }

            val items = (1L..3L).map { SourceRecord(id = it, name = "item-$it", value = it.toInt()) }
            writer.write(Chunk(items))

            val rows = SourceTable.selectAll().toList()
            val updated = rows.filter { it[SourceTable.name].endsWith("-updated") }
            updated.size shouldBeEqualTo 3
            updated.first()[SourceTable.value] shouldBeEqualTo 10
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 chunk 전달 시 아무 동작 안 함`(testDB: TestDB) {
        withBatchTables(testDB) {
            insertTestData(5)

            val writer = ExposedUpdateItemWriter<SourceRecord>(
                table = SourceTable,
                keyColumn = keyColumn,
                keyExtractor = { it.id },
            ) {
                this[SourceTable.name] = it.name + "-updated"
            }

            writer.write(Chunk(emptyList()))

            val rows = SourceTable.selectAll().toList()
            rows.none { it[SourceTable.name].endsWith("-updated") } shouldBeEqualTo true
        }
    }
}

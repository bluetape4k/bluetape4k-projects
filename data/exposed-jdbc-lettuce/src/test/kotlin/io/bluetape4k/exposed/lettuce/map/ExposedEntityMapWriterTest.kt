package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.WriteMode
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test

/**
 * [ExposedEntityMapWriter] 단위 테스트.
 */
class ExposedEntityMapWriterTest : AbstractExposedTest() {
    companion object : KLogging()

    private data class WriterEntity(
        override val id: Long,
        val name: String,
    ) : HasIdentifier<Long>

    private object WriterTable : LongIdTable("lettuce_writer_test") {
        val name = varchar("name", 64)
    }

    private fun ResultRow.toWriterEntity(): WriterEntity =
        WriterEntity(
            id = this[WriterTable.id].value,
            name = this[WriterTable.name]
        )

    private fun newWriter(writeMode: WriteMode = WriteMode.WRITE_THROUGH): ExposedEntityMapWriter<Long, WriterEntity> =
        ExposedEntityMapWriter(
            table = WriterTable,
            writeMode = writeMode,
            updateEntity = { stmt: UpdateStatement, entity: WriterEntity ->
                stmt[WriterTable.name] = entity.name
            },
            insertEntity = { stmt: BatchInsertStatement, entity: WriterEntity ->
                stmt[WriterTable.id] = entity.id
                stmt[WriterTable.name] = entity.name
            }
        )

    @Test
    fun `write - 새 엔티티를 DB에 삽입한다`() {
        withTables(TestDB.H2, WriterTable) {
            val writer = newWriter()

            val entity = WriterEntity(id = 1L, name = "alice")
            writer.write(mapOf(entity.id to entity))

            val rows = WriterTable.selectAll().toList()
            rows shouldHaveSize 1
            rows.first().toWriterEntity().name shouldBeEqualTo "alice"
        }
    }

    @Test
    fun `write - 기존 엔티티를 업데이트한다`() {
        withTables(TestDB.H2, WriterTable) {
            WriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }

            val writer = newWriter()
            writer.write(mapOf(1L to WriterEntity(id = 1L, name = "updated-alice")))

            val rows = WriterTable.selectAll().toList()
            rows shouldHaveSize 1
            rows.first().toWriterEntity().name shouldBeEqualTo "updated-alice"
        }
    }

    @Test
    fun `write - 빈 map은 아무것도 하지 않는다`() {
        withTables(TestDB.H2, WriterTable) {
            val writer = newWriter()
            writer.write(emptyMap())

            WriterTable.selectAll().toList().shouldHaveSize(0)
        }
    }

    @Test
    fun `write - NONE 모드에서는 DB에 쓰지 않는다`() {
        withTables(TestDB.H2, WriterTable) {
            val writer = newWriter(WriteMode.NONE)
            writer.write(mapOf(1L to WriterEntity(id = 1L, name = "alice")))

            WriterTable.selectAll().toList().shouldHaveSize(0)
        }
    }

    @Test
    fun `delete - 엔티티를 DB에서 삭제한다`() {
        withTables(TestDB.H2, WriterTable) {
            WriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }
            WriterTable.insert {
                it[id] = 2L
                it[name] = "bob"
            }

            val writer = newWriter()
            writer.delete(listOf(1L))

            val rows = WriterTable.selectAll().toList()
            rows shouldHaveSize 1
            rows.first().toWriterEntity().id shouldBeEqualTo 2L
        }
    }

    @Test
    fun `delete - 빈 컬렉션은 아무것도 하지 않는다`() {
        withTables(TestDB.H2, WriterTable) {
            WriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }

            val writer = newWriter()
            writer.delete(emptyList())

            WriterTable.selectAll().toList().shouldHaveSize(1)
        }
    }

    @Test
    fun `write - 신규와 기존 엔티티가 혼재할 때 각각 insert와 update를 수행한다`() {
        withTables(TestDB.H2, WriterTable) {
            WriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }

            val writer = newWriter()
            writer.write(
                mapOf(
                    1L to WriterEntity(id = 1L, name = "alice-updated"),
                    2L to WriterEntity(id = 2L, name = "bob-new")
                )
            )

            val rows = WriterTable.selectAll().sortedBy { it[WriterTable.id].value }
            rows shouldHaveSize 2
            rows[0].toWriterEntity().name shouldBeEqualTo "alice-updated"
            rows[1].toWriterEntity().name shouldBeEqualTo "bob-new"
        }
    }
}

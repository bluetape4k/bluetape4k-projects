package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.exposed.r2dbc.lettuce.AbstractR2dbcLettuceTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.WriteMode
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [R2dbcExposedEntityMapWriter] 단위 테스트.
 *
 * R2DBC `suspendTransaction` 기반으로 동작하며, `runBlocking` 없이 코루틴 네이티브로 실행한다.
 */
class R2dbcExposedEntityMapWriterTest: AbstractR2dbcLettuceTest() {
    companion object: KLoggingChannel()

    private data class WriterEntity(
        val id: Long,
        val name: String,
    )

    private object WriterTable: LongIdTable("r2dbc_lettuce_writer_test") {
        val name = varchar("name", 64)
    }

    private fun ResultRow.toWriterEntity(): WriterEntity =
        WriterEntity(
            id = this[WriterTable.id].value,
            name = this[WriterTable.name]
        )

    private fun newWriter(
        writeMode: WriteMode = WriteMode.WRITE_THROUGH,
    ): R2dbcExposedEntityMapWriter<Long, WriterEntity> =
        R2dbcExposedEntityMapWriter(
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

    /** writer 동작 후 DB의 모든 행을 별도 트랜잭션에서 조회한다 */
    private suspend fun allRows(): List<WriterEntity> =
        suspendTransaction {
            WriterTable.selectAll().map { it.toWriterEntity() }.toList()
        }

    @Test
    fun `write - 새 엔티티를 DB에 삽입한다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                val writer = newWriter()

                val entity = WriterEntity(id = 1L, name = "alice")
                writer.write(mapOf(entity.id to entity))

                val rows = allRows()
                rows shouldHaveSize 1
                rows.first().name shouldBeEqualTo "alice"
            }
        }

    @Test
    fun `write - 기존 엔티티를 업데이트한다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                val insertedId = WriterTable.insertAndGetId { it[name] = "alice" }.value
                commit()

                val writer = newWriter()
                writer.write(mapOf(insertedId to WriterEntity(id = insertedId, name = "updated-alice")))

                val rows = allRows()
                rows shouldHaveSize 1
                rows.first().name shouldBeEqualTo "updated-alice"
            }
        }

    @Test
    fun `write - 빈 map은 아무것도 하지 않는다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                val writer = newWriter()
                writer.write(emptyMap())

                allRows() shouldHaveSize 0
            }
        }

    @Test
    fun `write - NONE 모드에서는 DB에 쓰지 않는다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                val writer = newWriter(WriteMode.NONE)
                writer.write(mapOf(1L to WriterEntity(id = 1L, name = "alice")))

                allRows() shouldHaveSize 0
            }
        }

    @Test
    fun `delete - 엔티티를 DB에서 삭제한다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                val id1 = WriterTable.insertAndGetId { it[name] = "alice" }.value
                val id2 = WriterTable.insertAndGetId { it[name] = "bob" }.value
                commit()

                val writer = newWriter()
                writer.delete(listOf(id1))

                val rows = allRows()
                rows shouldHaveSize 1
                rows.first().id shouldBeEqualTo id2
            }
        }

    @Test
    fun `delete - 빈 컬렉션은 아무것도 하지 않는다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                WriterTable.insert { it[name] = "alice" }
                commit()

                val writer = newWriter()
                writer.delete(emptyList())

                allRows() shouldHaveSize 1
            }
        }

    @Test
    fun `write - 신규와 기존 엔티티가 혼재할 때 각각 insert와 update를 수행한다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                val existingId = WriterTable.insertAndGetId { it[name] = "alice" }.value
                commit()

                val newId = existingId + 1000L
                val writer = newWriter()
                writer.write(
                    mapOf(
                        existingId to WriterEntity(id = existingId, name = "alice-updated"),
                        newId to WriterEntity(id = newId, name = "bob-new")
                    )
                )

                val rows = allRows().sortedBy { it.id }
                rows shouldHaveSize 2
                rows[0].name shouldBeEqualTo "alice-updated"
                rows[1].name shouldBeEqualTo "bob-new"
            }
        }

    @Test
    fun `chunkSize는 0보다 커야 한다`() =
        runTest {
            withTables(TestDB.H2, WriterTable) {
                assertFailsWith<IllegalArgumentException> {
                    R2dbcExposedEntityMapWriter(
                        table = WriterTable,
                        writeMode = WriteMode.WRITE_THROUGH,
                        chunkSize = 0,
                        updateEntity = { stmt: UpdateStatement, entity: WriterEntity ->
                            stmt[WriterTable.name] = entity.name
                        },
                        insertEntity = { stmt: BatchInsertStatement, entity: WriterEntity ->
                            stmt[WriterTable.id] = entity.id
                            stmt[WriterTable.name] = entity.name
                        }
                    )
                }
            }
        }
}

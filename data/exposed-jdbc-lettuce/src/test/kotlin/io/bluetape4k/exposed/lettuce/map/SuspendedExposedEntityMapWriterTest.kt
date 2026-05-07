package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTablesSuspending
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.WriteMode
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import java.io.Serializable
import io.bluetape4k.assertions.assertFailsWith

/**
 * [SuspendedExposedEntityMapWriter] 단위 테스트.
 *
 * [SuspendedEntityMapWriter.write]/[SuspendedEntityMapWriter.delete]는 내부적으로
 * [suspendedTransactionAsync]를 열어 별도 트랜잭션에서 쓴다.
 * 사전에 삽입한 데이터는 반드시 `commit()` 후에 writer가 볼 수 있고,
 * writer가 커밋한 데이터는 이후 같은 외부 트랜잭션에서 READ_COMMITTED로 볼 수 있다.
 */
class SuspendedExposedEntityMapWriterTest: AbstractExposedTest() {
    companion object: KLogging()

    private data class SuspendedWriterEntity(
        val id: Long,
        val name: String,
    ): Serializable

    /** 클라이언트 생성 ID 테이블 (AutoInc 아님) — Writer 삽입 동작을 직접 테스트하기 위해 사용 */
    private object SuspendedWriterTable: IdTable<Long>("suspended_writer_test") {
        override val id: Column<EntityID<Long>> = long("id").entityId()
        val name = varchar("name", 64)
        override val primaryKey = PrimaryKey(id)
    }

    private fun ResultRow.toSuspendedWriterEntity(): SuspendedWriterEntity =
        SuspendedWriterEntity(
            id = this[SuspendedWriterTable.id].value,
            name = this[SuspendedWriterTable.name]
        )

    private fun newWriter(
        writeMode: WriteMode = WriteMode.WRITE_THROUGH,
    ): SuspendedExposedEntityMapWriter<Long, SuspendedWriterEntity> =
        SuspendedExposedEntityMapWriter(
            table = SuspendedWriterTable,
            writeMode = writeMode,
            updateEntity = { stmt: UpdateStatement, entity: SuspendedWriterEntity ->
                stmt[SuspendedWriterTable.name] = entity.name
            },
            insertEntity = { stmt: BatchInsertStatement, entity: SuspendedWriterEntity ->
                stmt[SuspendedWriterTable.id] = entity.id
                stmt[SuspendedWriterTable.name] = entity.name
            }
        )

    @Test
    fun `write - 새 엔티티를 DB에 삽입한다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedWriterTable) {
            val writer = newWriter()
            val entity = SuspendedWriterEntity(id = 1L, name = "alice")
            // write()는 별도 트랜잭션에서 커밋한다
            writer.write(mapOf(entity.id to entity))

            val rows = SuspendedWriterTable.selectAll().toList()
            rows shouldHaveSize 1
            rows.first().toSuspendedWriterEntity().name shouldBeEqualTo "alice"
        }
    }

    @Test
    fun `write - 기존 엔티티를 업데이트한다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedWriterTable) {
            SuspendedWriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }
            // writer의 새 트랜잭션에서 기존 데이터를 보려면 커밋 필요
            commit()

            val writer = newWriter()
            writer.write(mapOf(1L to SuspendedWriterEntity(id = 1L, name = "updated-alice")))

            val rows = SuspendedWriterTable.selectAll().toList()
            rows shouldHaveSize 1
            rows.first().toSuspendedWriterEntity().name shouldBeEqualTo "updated-alice"
        }
    }

    @Test
    fun `write - 빈 map은 아무것도 하지 않는다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedWriterTable) {
            val writer = newWriter()
            writer.write(emptyMap())

            SuspendedWriterTable.selectAll().toList().shouldHaveSize(0)
        }
    }

    @Test
    fun `write - NONE 모드에서는 DB에 쓰지 않는다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedWriterTable) {
            val writer = newWriter(WriteMode.NONE)
            writer.write(mapOf(1L to SuspendedWriterEntity(id = 1L, name = "alice")))

            SuspendedWriterTable.selectAll().toList().shouldHaveSize(0)
        }
    }

    @Test
    fun `delete - 엔티티를 DB에서 삭제한다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedWriterTable) {
            SuspendedWriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }
            SuspendedWriterTable.insert {
                it[id] = 2L
                it[name] = "bob"
            }
            // writer의 새 트랜잭션에서 기존 데이터를 보려면 커밋 필요
            commit()

            val writer = newWriter()
            writer.delete(listOf(1L))

            val rows = SuspendedWriterTable.selectAll().toList()
            rows shouldHaveSize 1
            rows.first().toSuspendedWriterEntity().id shouldBeEqualTo 2L
        }
    }

    @Test
    fun `delete - 빈 컬렉션은 아무것도 하지 않는다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedWriterTable) {
            SuspendedWriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }
            commit()

            val writer = newWriter()
            writer.delete(emptyList())

            SuspendedWriterTable.selectAll().toList().shouldHaveSize(1)
        }
    }

    @Test
    fun `write - 신규와 기존 엔티티가 혼재할 때 각각 insert와 update를 수행한다`() = runTest {
        withTablesSuspending(TestDB.H2, SuspendedWriterTable) {
            SuspendedWriterTable.insert {
                it[id] = 1L
                it[name] = "alice"
            }
            // writer의 새 트랜잭션에서 기존 데이터를 보려면 커밋 필요
            commit()

            val writer = newWriter()
            writer.write(
                mapOf(
                    1L to SuspendedWriterEntity(id = 1L, name = "alice-updated"),
                    2L to SuspendedWriterEntity(id = 2L, name = "bob-new")
                )
            )

            val rows = SuspendedWriterTable.selectAll().sortedBy { it[SuspendedWriterTable.id].value }
            rows shouldHaveSize 2
            rows[0].toSuspendedWriterEntity().name shouldBeEqualTo "alice-updated"
            rows[1].toSuspendedWriterEntity().name shouldBeEqualTo "bob-new"
        }
    }

    @Test
    fun `chunkSize는 0보다 커야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            SuspendedExposedEntityMapWriter(
                table = SuspendedWriterTable,
                writeMode = WriteMode.WRITE_THROUGH,
                chunkSize = 0,
                updateEntity = { stmt: UpdateStatement, entity: SuspendedWriterEntity ->
                    stmt[SuspendedWriterTable.name] = entity.name
                },
                insertEntity = { stmt: BatchInsertStatement, entity: SuspendedWriterEntity ->
                    stmt[SuspendedWriterTable.id] = entity.id
                    stmt[SuspendedWriterTable.name] = entity.name
                }
            )
        }
    }
}

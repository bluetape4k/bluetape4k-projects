package io.bluetape4k.batch.r2dbc

import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.batch.TargetRecord
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [ExposedR2dbcBatchWriter] R2DBC 통합 테스트.
 *
 * H2 / PostgreSQL / MySQL 각 방언에서:
 * 1. 정상 batchInsert
 * 2. 빈 리스트 → no-op
 * 3. 다회 write 누적
 */
class ExposedR2dbcBatchWriterTest : AbstractBatchR2dbcTest() {

    private fun makeWriter(database: R2dbcDatabase): ExposedR2dbcBatchWriter<TargetRecord> =
        ExposedR2dbcBatchWriter(
            database = database,
            table = BatchTargetTable,
        ) { record ->
            this[BatchTargetTable.sourceName] = record.sourceName
            this[BatchTargetTable.transformedValue] = record.transformedValue
        }

    // ─── 1. 정상 batchInsert ─────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `정상 batchInsert - 레코드가 DB에 저장됨`(testDB: TestDB) {
        runSuspendIO {
            withBatchTables(testDB) { db ->
                val writer = makeWriter(db.db!!)
                val items = (1..10).map { TargetRecord("item-$it", it * 2) }

                writer.write(items)

                BatchTargetTable.selectAll().count() shouldBeEqualTo 10L
            }
        }
    }

    // ─── 2. 빈 리스트 → no-op ────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 리스트 - no-op, DB 변경 없음`(testDB: TestDB) {
        runSuspendIO {
            withBatchTables(testDB) { db ->
                val writer = makeWriter(db.db!!)

                writer.write(emptyList())

                BatchTargetTable.selectAll().count() shouldBeEqualTo 0L
            }
        }
    }

    // ─── 3. 다회 write 누적 ──────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `다회 write - 각 청크가 누적 저장됨`(testDB: TestDB) {
        runSuspendIO {
            withBatchTables(testDB) { db ->
                val writer = makeWriter(db.db!!)
                val chunk1 = (1..5).map { TargetRecord("a-$it", it) }
                val chunk2 = (6..10).map { TargetRecord("b-$it", it) }

                writer.write(chunk1)
                writer.write(chunk2)

                BatchTargetTable.selectAll().count() shouldBeEqualTo 10L
            }
        }
    }
}

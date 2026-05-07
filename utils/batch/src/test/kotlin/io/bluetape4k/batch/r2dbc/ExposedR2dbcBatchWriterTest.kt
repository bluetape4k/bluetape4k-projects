package io.bluetape4k.batch.r2dbc

import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.batch.TargetRecord
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.assertions.shouldBeEqualTo
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import io.bluetape4k.assertions.assertFailsWith

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

    // ─── 3. 중복 키 INSERT 시 예외 전파 ─────────────────────────────────────────

    /**
     * `ignore` 옵션 없이 unique 컬럼에 중복 삽입 시 R2DBC 예외가 전파됨을 검증한다.
     *
     * [BatchTargetTable.sourceName]은 unique 인덱스를 가지므로 동일 값을 두 번 삽입하면
     * [Exception]이 발생해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `중복 키 INSERT 시 예외 전파`(testDB: TestDB) {
        runSuspendIO {
            withBatchTables(testDB) { db ->
                val writer = makeWriter(db.db!!)
                val items = listOf(TargetRecord("r2dbc-dup", 99))

                writer.write(items)

                // 두 번째 동일 키 삽입 → R2DBC SQL 예외 전파
                assertFailsWith<Exception> {
                    writer.write(items)
                }

                BatchTargetTable.selectAll().count() shouldBeEqualTo 1L
            }
        }
    }

    // ─── 4. 다회 write 누적 ──────────────────────────────────────────────────

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

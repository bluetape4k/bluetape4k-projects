package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.batch.TargetRecord
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [ExposedJdbcBatchWriter] 통합 테스트.
 *
 * H2 / PostgreSQL / MySQL 각 방언에서:
 * 1. 정상 batchInsert
 * 2. 빈 리스트 → no-op
 * 3. 중복 키 (ignore = true)
 * 4. 다회 write 누적
 */
class ExposedJdbcBatchWriterTest : AbstractBatchJdbcTest() {

    private fun makeWriter(testDB: TestDB, ignore: Boolean = false): ExposedJdbcBatchWriter<TargetRecord> =
        ExposedJdbcBatchWriter(
            database = testDB.db!!,
            table = BatchTargetTable,
            ignore = ignore,
        ) { record ->
            this[BatchTargetTable.sourceName] = record.sourceName
            this[BatchTargetTable.transformedValue] = record.transformedValue
        }

    private fun countTarget(testDB: TestDB): Long =
        transaction(testDB.db!!) {
            BatchTargetTable.selectAll().count()
        }

    // ─── 1. 정상 batchInsert ─────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `정상 batchInsert - 레코드가 DB에 저장됨`(testDB: TestDB) {
        withBatchTables(testDB) {
            val writer = makeWriter(testDB)
            val items = (1..10).map { TargetRecord("item-$it", it * 2) }

            runSuspendIO { writer.write(items) }

            countTarget(testDB) shouldBeEqualTo 10L
        }
    }

    // ─── 2. 빈 리스트 → no-op ────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 리스트 - no-op, DB 변경 없음`(testDB: TestDB) {
        withBatchTables(testDB) {
            val writer = makeWriter(testDB)

            runSuspendIO { writer.write(emptyList()) }

            countTarget(testDB) shouldBeEqualTo 0L
        }
    }

    // ─── 3. 다회 write 누적 ──────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `다회 write - 각 청크가 누적 저장됨`(testDB: TestDB) {
        withBatchTables(testDB) {
            val writer = makeWriter(testDB)
            val chunk1 = (1..5).map { TargetRecord("a-$it", it) }
            val chunk2 = (6..10).map { TargetRecord("b-$it", it) }

            runSuspendIO {
                writer.write(chunk1)
                writer.write(chunk2)
            }

            countTarget(testDB) shouldBeEqualTo 10L
        }
    }

    // ─── 4. ignore = true - 중복 키 무시 ─────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ignore = true - 중복 키 INSERT IGNORE 동작`(testDB: TestDB) {
        assumeTrue(testDB != TestDB.H2, "H2 does not support INSERT IGNORE / ON CONFLICT DO NOTHING for unique index")
        withBatchTables(testDB) {
            val writer = makeWriter(testDB, ignore = true)
            val items = listOf(TargetRecord("dup", 1))

            runSuspendIO { writer.write(items) }
            runSuspendIO { writer.write(items) }  // 중복 → 무시

            countTarget(testDB) shouldBeEqualTo 1L
        }
    }
}

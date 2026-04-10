package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.BatchSourceTable
import io.bluetape4k.batch.SourceRecord
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [ExposedJdbcBatchReader] 통합 테스트.
 *
 * H2 / PostgreSQL / MySQL 각 방언에서:
 * 1. keyset 페이지네이션 전체 읽기
 * 2. 빈 테이블 → 즉시 EOF
 * 3. pageSize 경계 (정확히 배수 / 배수+1)
 * 4. 체크포인트 재시작 (restoreFrom)
 * 5. onChunkCommitted 후 checkpoint() 값 검증
 *
 * 참고: reader 의 open/read/close 는 suspend 함수이므로 runSuspendIO 내부에서 호출하며,
 * 데이터 삽입도 동일 runSuspendIO 블록 안에서 transaction을 통해 commit 후 읽는다.
 */
class ExposedJdbcBatchReaderTest : AbstractBatchJdbcTest() {

    private fun makeReader(testDB: TestDB, pageSize: Int = 10): ExposedJdbcBatchReader<Long, SourceRecord> =
        ExposedJdbcBatchReader(
            database = testDB.db!!,
            table = BatchSourceTable,
            keyColumn = BatchSourceTable.id,
            pageSize = pageSize,
            rowMapper = { row ->
                SourceRecord(
                    id = row[BatchSourceTable.id],
                    name = row[BatchSourceTable.name],
                    value = row[BatchSourceTable.value],
                )
            },
            keyExtractor = { it.id },
        )

    // ─── 1. 전체 읽기 ────────────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `전체 읽기 - 모든 레코드를 keyset 페이지네이션으로 읽음`(testDB: TestDB) {
        withBatchTables(testDB) {
            val results = mutableListOf<SourceRecord>()
            runSuspendIO {
                transaction(testDB.db!!) {
                    BatchSourceTable.batchInsert((1..25).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }
                val reader = makeReader(testDB, pageSize = 10)
                reader.open()
                var item = reader.read()
                while (item != null) {
                    results.add(item)
                    item = reader.read()
                }
                reader.close()
            }
            results.size shouldBeEqualTo 25
            results.map { it.id } shouldBeEqualTo (1L..25L).toList()
        }
    }

    // ─── 2. 빈 테이블 ────────────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 테이블 - 첫 read에서 null 반환`(testDB: TestDB) {
        withBatchTables(testDB) {
            var result: SourceRecord? = null
            runSuspendIO {
                val reader = makeReader(testDB)
                reader.open()
                result = reader.read()
                reader.close()
            }
            result.shouldBeNull()
        }
    }

    // ─── 3. pageSize 정확히 배수 ─────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `pageSize 정확히 배수 - 모든 페이지 반환 후 EOF`(testDB: TestDB) {
        withBatchTables(testDB) {
            var count = 0
            runSuspendIO {
                transaction(testDB.db!!) {
                    BatchSourceTable.batchInsert((1..20).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }
                val reader = makeReader(testDB, pageSize = 10)
                reader.open()
                while (reader.read() != null) count++
                reader.close()
            }
            count shouldBeEqualTo 20
        }
    }

    // ─── 4. 체크포인트 재시작 ─────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `체크포인트 재시작 - restoreFrom 후 이후 레코드만 읽음`(testDB: TestDB) {
        withBatchTables(testDB) {
            var cp: Any? = null
            val results2 = mutableListOf<SourceRecord>()

            runSuspendIO {
                transaction(testDB.db!!) {
                    BatchSourceTable.batchInsert((1..30).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }
                // 첫 10개 읽기 후 checkpoint 저장
                val reader = makeReader(testDB, pageSize = 10)
                reader.open()
                repeat(10) { reader.read() }
                reader.onChunkCommitted()
                cp = reader.checkpoint()
                reader.close()
            }

            runSuspendIO {
                // 체크포인트 복원 후 나머지 20개 읽기
                val reader2 = makeReader(testDB, pageSize = 10)
                reader2.open()
                reader2.restoreFrom(cp!!)
                var item = reader2.read()
                while (item != null) {
                    results2.add(item)
                    item = reader2.read()
                }
                reader2.close()
            }

            cp.shouldNotBeNull()
            results2.size shouldBeEqualTo 20
            results2.first().id shouldBeEqualTo 11L
        }
    }

    // ─── 5. onChunkCommitted → checkpoint 값 검증 ─────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `onChunkCommitted 전후 checkpoint 값 검증`(testDB: TestDB) {
        withBatchTables(testDB) {
            var cp: Any? = null
            runSuspendIO {
                transaction(testDB.db!!) {
                    BatchSourceTable.batchInsert((1..5).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }
                val reader = makeReader(testDB, pageSize = 5)
                reader.open()
                // 커밋 전 checkpoint는 null
                reader.checkpoint().shouldBeNull()
                // 전체 읽기
                repeat(5) { reader.read() }
                // 커밋 후 checkpoint = 5 (마지막 레코드 ID)
                reader.onChunkCommitted()
                cp = reader.checkpoint()
                reader.close()
            }
            cp.shouldNotBeNull()
            (cp as Long) shouldBeEqualTo 5L
        }
    }

    // ─── 6. pageSize + 1 경계 ────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `pageSize + 1 경계 - 마지막 페이지가 1개일 때 정상 동작`(testDB: TestDB) {
        withBatchTables(testDB) {
            var count = 0
            runSuspendIO {
                transaction(testDB.db!!) {
                    BatchSourceTable.batchInsert((1..11).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }
                val reader = makeReader(testDB, pageSize = 10)
                reader.open()
                while (reader.read() != null) count++
                reader.close()
            }
            count shouldBeEqualTo 11
        }
    }
}

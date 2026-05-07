package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseRecord
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseRepository
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseTable
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.withEdgeCaseTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * JdbcRepository의 write 계열 엣지 케이스 테스트.
 *
 * batchUpsert(Iterable/Sequence), onUpdate 람다, onUpdateExclude,
 * updateAll, deleteAll(op), deleteAllByIds, deleteAllIgnore(MySQL only),
 * deleteByIdIgnore(MySQL only), batchInsert(Sequence) 동작을 검증한다.
 */
class JdbcRepositoryWriteEdgeCaseTest : AbstractExposedTest() {

    companion object : KLogging()

    // ── batchUpsert ─────────────────────────────────────────────────────────

    /**
     * batchUpsert(Iterable) 는 테이블에 없는 name 을 가진 레코드를 INSERT 해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert(Iterable) 는 신규 record INSERT`(testDB: TestDB) {
        // MySQL은 UPSERT에서 conflict key 지정을 지원하지 않음
        Assumptions.assumeTrue(testDB != TestDB.MYSQL_V8)
        withEdgeCaseTable(testDB) {
            val records = listOf(
                EdgeCaseRecord(name = "Alice", age = 30, isActive = true),
                EdgeCaseRecord(name = "Bob",   age = 25, isActive = false),
            )

            val result = EdgeCaseRepository.batchUpsert(
                records,
                keys = arrayOf(EdgeCaseTable.name),
            ) { rec ->
                this[EdgeCaseTable.name]     = rec.name
                this[EdgeCaseTable.age]      = rec.age
                this[EdgeCaseTable.isActive] = rec.isActive
            }

            result shouldHaveSize 2
            result.all { it.id > 0L }.shouldBeTrue()

            EdgeCaseRepository.count() shouldBeEqualTo 2L
        }
    }

    /**
     * batchUpsert(Iterable) 는 name UNIQUE 충돌 시 해당 row 를 UPDATE 해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert(Iterable) 는 기존 record UPDATE`(testDB: TestDB) {
        // MySQL은 UPSERT에서 conflict key 지정을 지원하지 않음
        Assumptions.assumeTrue(testDB != TestDB.MYSQL_V8)
        withEdgeCaseTable(testDB) {
            // 사전 삽입
            val saved = EdgeCaseRepository.save(EdgeCaseRecord(name = "Alice", age = 30, isActive = true))
            saved.id shouldBeGreaterThan 0L

            // name 충돌 → UPDATE (age 변경)
            val upsertRecords = listOf(
                EdgeCaseRecord(name = "Alice", age = 99, isActive = false),
            )
            val result = EdgeCaseRepository.batchUpsert(
                upsertRecords,
                keys = arrayOf(EdgeCaseTable.name),
            ) { rec ->
                this[EdgeCaseTable.name]     = rec.name
                this[EdgeCaseTable.age]      = rec.age
                this[EdgeCaseTable.isActive] = rec.isActive
            }

            result shouldHaveSize 1
            EdgeCaseRepository.count() shouldBeEqualTo 1L

            val updated = EdgeCaseRepository.findById(result.first().id)
            updated.name     shouldBeEqualTo "Alice"
            updated.age      shouldBeEqualTo 99
            updated.isActive shouldBeEqualTo false
        }
    }

    /**
     * batchUpsert(Sequence) 는 lazy 평가로 신규 INSERT 및 기존 UPDATE 를 모두 처리해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert(Sequence) 는 lazy 평가로 INSERT·UPDATE`(testDB: TestDB) {
        // MySQL은 UPSERT에서 conflict key 지정을 지원하지 않음
        Assumptions.assumeTrue(testDB != TestDB.MYSQL_V8)
        withEdgeCaseTable(testDB) {
            // 사전 삽입
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Seq-Existing", age = 10))

            val records = sequenceOf(
                EdgeCaseRecord(name = "Seq-Existing", age = 20),  // UPDATE
                EdgeCaseRecord(name = "Seq-New",      age = 30),  // INSERT
            )

            val result = EdgeCaseRepository.batchUpsert(
                records,
                keys = arrayOf(EdgeCaseTable.name),
            ) { rec ->
                this[EdgeCaseTable.name]     = rec.name
                this[EdgeCaseTable.age]      = rec.age
                this[EdgeCaseTable.isActive] = rec.isActive
            }

            result shouldHaveSize 2
            EdgeCaseRepository.count() shouldBeEqualTo 2L
        }
    }

    /**
     * batchUpsert 의 onUpdate 람다를 사용하면 충돌 시 특정 컬럼만 UPDATE 할 수 있다.
     *
     * isActive 는 onUpdate 에서 지정하지 않으므로 기존 값(true)이 유지되어야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert 의 onUpdate 람다로 충돌 시 컬럼 값 제어`(testDB: TestDB) {
        // MySQL은 UPSERT에서 conflict key 지정을 지원하지 않음
        Assumptions.assumeTrue(testDB != TestDB.MYSQL_V8)
        withEdgeCaseTable(testDB) {
            // 사전 삽입: isActive = true
            EdgeCaseRepository.save(EdgeCaseRecord(name = "OnUpdate-Test", age = 10, isActive = true))

            val records = listOf(
                EdgeCaseRecord(name = "OnUpdate-Test", age = 99, isActive = false),
            )

            // onUpdate: age 만 insertValue 로 갱신, isActive 는 명시하지 않음
            EdgeCaseRepository.batchUpsert(
                records,
                keys = arrayOf(EdgeCaseTable.name),
                onUpdate = { update ->
                    update[EdgeCaseTable.age] = insertValue(EdgeCaseTable.age)
                },
            ) { rec ->
                this[EdgeCaseTable.name]     = rec.name
                this[EdgeCaseTable.age]      = rec.age
                this[EdgeCaseTable.isActive] = rec.isActive
            }

            val found = EdgeCaseRepository.findAll { EdgeCaseTable.name eq "OnUpdate-Test" }
            found shouldHaveSize 1
            found.single().age shouldBeEqualTo 99
            // isActive 는 onUpdate 에 포함되지 않았으므로 기존 값(true) 유지
            found.single().isActive.shouldBeTrue()
        }
    }

    /**
     * batchUpsert 의 onUpdateExclude 를 사용하면 충돌 시 특정 컬럼이 UPDATE 에서 제외된다.
     *
     * isActive 를 onUpdateExclude 에 포함하면 기존 isActive 값이 보존되어야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert 의 onUpdateExclude 로 일부 컬럼 보존`(testDB: TestDB) {
        // MySQL은 UPSERT에서 conflict key 지정을 지원하지 않음
        Assumptions.assumeTrue(testDB != TestDB.MYSQL_V8)
        withEdgeCaseTable(testDB) {
            // 사전 삽입: isActive = true
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Exclude-Test", age = 5, isActive = true))

            val records = listOf(
                EdgeCaseRecord(name = "Exclude-Test", age = 77, isActive = false),
            )

            EdgeCaseRepository.batchUpsert(
                records,
                keys = arrayOf(EdgeCaseTable.name),
                onUpdateExclude = listOf(EdgeCaseTable.isActive),
            ) { rec ->
                this[EdgeCaseTable.name]     = rec.name
                this[EdgeCaseTable.age]      = rec.age
                this[EdgeCaseTable.isActive] = rec.isActive
            }

            val found = EdgeCaseRepository.findAll { EdgeCaseTable.name eq "Exclude-Test" }
            found shouldHaveSize 1
            found.single().age shouldBeEqualTo 77
            // isActive 는 exclude 로 제외했으므로 기존 값(true) 유지
            found.single().isActive.shouldBeTrue()
        }
    }

    // ── updateAll ────────────────────────────────────────────────────────────

    /**
     * updateAll 은 조건에 맞는 row 만 업데이트하고 다른 row 는 변경하지 않아야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `updateAll 은 조건에 맞는 row 만 업데이트`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Update-A", age = 10, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Update-B", age = 20, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Update-C", age = 30, isActive = false))

            // isActive = true 인 row 의 age 를 99 로 변경
            EdgeCaseRepository.updateAll(predicate = { EdgeCaseTable.isActive eq true }) {
                it[EdgeCaseTable.age] = 99
            }

            val activeRows = EdgeCaseRepository.findAll { EdgeCaseTable.isActive eq true }
            activeRows.all { it.age == 99 }.shouldBeTrue()

            val inactiveRows = EdgeCaseRepository.findAll { EdgeCaseTable.isActive eq false }
            inactiveRows.all { it.age == 30 }.shouldBeTrue()
        }
    }

    /**
     * updateAll 은 실제로 업데이트된 row 수를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `updateAll 결과로 영향받은 row 수 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Count-A", age = 1, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Count-B", age = 2, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Count-C", age = 3, isActive = false))

            val updatedCount = EdgeCaseRepository.updateAll(
                predicate = { EdgeCaseTable.isActive eq true },
            ) {
                it[EdgeCaseTable.age] = 100
            }

            updatedCount shouldBeEqualTo 2
        }
    }

    // ── deleteAll ────────────────────────────────────────────────────────────

    /**
     * deleteAll(op) 은 조건에 맞는 row 만 삭제하고 다른 row 는 유지해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `deleteAll(op) 은 조건 매칭 row 만 삭제`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Del-A", age = 10, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Del-B", age = 20, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Del-C", age = 30, isActive = false))

            val deleted = EdgeCaseRepository.deleteAll { EdgeCaseTable.isActive eq false }

            deleted shouldBeEqualTo 1
            EdgeCaseRepository.count() shouldBeEqualTo 2L
            EdgeCaseRepository.findAll { EdgeCaseTable.name eq "Del-C" } shouldHaveSize 0
        }
    }

    /**
     * deleteAllByIds 는 전달한 id 리스트의 row 를 모두 삭제해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `deleteAllByIds 는 id 리스트 모두 삭제`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            val r1 = EdgeCaseRepository.save(EdgeCaseRecord(name = "Ids-A", age = 1))
            val r2 = EdgeCaseRepository.save(EdgeCaseRecord(name = "Ids-B", age = 2))
            val r3 = EdgeCaseRepository.save(EdgeCaseRecord(name = "Ids-C", age = 3))

            val deleted = EdgeCaseRepository.deleteAllByIds(listOf(r1.id, r2.id))

            deleted shouldBeEqualTo 2
            EdgeCaseRepository.count() shouldBeEqualTo 1L
            EdgeCaseRepository.findById(r3.id).shouldNotBeNull()
        }
    }

    // ── deleteAllIgnore / deleteByIdIgnore (MySQL only) ──────────────────────

    /**
     * deleteAllIgnore 는 IGNORE 절로 실패를 무시하고 삭제를 수행해야 한다.
     *
     * MySQL / MariaDB 에서만 지원되며, 다른 DB 에서는 테스트를 건너뛴다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `deleteAllIgnore 는 IGNORE 절로 실패 무시`(testDB: TestDB) {
        Assumptions.assumeTrue(testDB in TestDB.ALL_MYSQL_MARIADB)

        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Ignore-A", age = 10, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Ignore-B", age = 20, isActive = false))

            val deleted = EdgeCaseRepository.deleteAllIgnore { EdgeCaseTable.isActive eq true }

            deleted shouldBeEqualTo 1
            EdgeCaseRepository.count() shouldBeEqualTo 1L
        }
    }

    /**
     * deleteByIdIgnore 는 단건 IGNORE 삭제를 수행해야 한다.
     *
     * MySQL / MariaDB 에서만 지원되며, 다른 DB 에서는 테스트를 건너뛴다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `deleteByIdIgnore 는 단건 IGNORE 삭제`(testDB: TestDB) {
        Assumptions.assumeTrue(testDB in TestDB.ALL_MYSQL_MARIADB)

        withEdgeCaseTable(testDB) {
            val saved = EdgeCaseRepository.save(EdgeCaseRecord(name = "IgnoreById", age = 5))
            saved.id shouldBeGreaterThan 0L

            // 존재하는 id 삭제 → 1 반환
            val deleted = EdgeCaseRepository.deleteByIdIgnore(saved.id)
            deleted shouldBeEqualTo 1

            // 이미 삭제된 id → 0 반환 (예외 없음)
            val deletedAgain = EdgeCaseRepository.deleteByIdIgnore(saved.id)
            deletedAgain shouldBeEqualTo 0
        }
    }

    // ── batchInsert(Sequence) ────────────────────────────────────────────────

    /**
     * batchInsert(Sequence) 는 Sequence 를 lazy 평가하여 모든 레코드를 INSERT 해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchInsert(Sequence) 는 lazy 평가로 처리`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            val batchCount = 10
            val records = (1..batchCount).asSequence().map { i ->
                EdgeCaseRecord(name = "Seq-$i", age = i * 10, isActive = i % 2 == 0)
            }

            val inserted = EdgeCaseRepository.batchInsert(records) { rec ->
                this[EdgeCaseTable.name]     = rec.name
                this[EdgeCaseTable.age]      = rec.age
                this[EdgeCaseTable.isActive] = rec.isActive
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()
            EdgeCaseRepository.count() shouldBeEqualTo batchCount.toLong()

            // 짝수 인덱스는 isActive = true
            inserted.filter { it.isActive }.size shouldBeEqualTo batchCount / 2
        }
    }
}

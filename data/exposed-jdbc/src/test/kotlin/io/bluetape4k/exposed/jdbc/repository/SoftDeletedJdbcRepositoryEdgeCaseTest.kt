package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable

/**
 * [SoftDeletedJdbcRepository] 엣지 케이스 테스트입니다.
 *
 * `countActive`, `countDeleted`, `softDeleteAll`, `restoreAll`, `findActivePage`, `findDeleted`의
 * 경계 조건 및 비정상 입력 케이스를 검증합니다:
 * - 빈 테이블에서 count 메서드 호출
 * - 매칭되지 않는 predicate로 softDeleteAll/restoreAll 호출
 * - 전체 삭제 후 복원 전후 countActive/countDeleted 검증
 * - findActivePage 결과가 soft-delete 필터를 올바르게 적용하는지
 * - findDeleted에 추가 predicate 조합 적용
 */
class SoftDeletedJdbcRepositoryEdgeCaseTest : AbstractExposedTest() {

    companion object : KLogging()

    // ── 테이블 정의 ────────────────────────────────────────────────────────────────

    object EdgeCaseTable : SoftDeletedIdTable<Long>("soft_deleted_edge_items") {
        override val id: Column<EntityID<Long>> = long("id").autoIncrement().entityId()
        val name = varchar("name", 255)
        val category = varchar("category", 100).default("default")
        override val primaryKey = PrimaryKey(id)
    }

    // ── 레코드 타입 ─────────────────────────────────────────────────────────────────

    data class EdgeCaseRecord(
        val id: Long = 0L,
        val name: String,
        val category: String = "default",
        val isDeleted: Boolean = false,
    ) : Serializable

    // ── Repository 구현 ─────────────────────────────────────────────────────────────

    val repository = object : LongSoftDeletedJdbcRepository<EdgeCaseRecord, EdgeCaseTable> {
        override val table: EdgeCaseTable = EdgeCaseTable

        override fun extractId(entity: EdgeCaseRecord): Long = entity.id

        override fun ResultRow.toEntity(): EdgeCaseRecord = EdgeCaseRecord(
            id = this[EdgeCaseTable.id].value,
            name = this[EdgeCaseTable.name],
            category = this[EdgeCaseTable.category],
            isDeleted = this[EdgeCaseTable.isDeleted],
        )
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────────────────────

    private fun insertRecord(name: String, category: String = "default"): Long =
        EdgeCaseTable.insertAndGetId {
            it[EdgeCaseTable.name] = name
            it[EdgeCaseTable.category] = category
        }.value

    // ── 테스트 ─────────────────────────────────────────────────────────────────────

    /**
     * 빈 테이블에서 `countActive`는 0을 반환하고, `countDeleted`도 0을 반환해야 합니다.
     *
     * 테이블에 데이터가 없을 때 count 메서드가 예외 없이 0을 반환하는지 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 테이블에서 countActive 와 countDeleted 는 0 을 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            repository.countActive() shouldBeEqualTo 0L
            repository.countDeleted() shouldBeEqualTo 0L
        }
    }

    /**
     * `countActive`와 `countDeleted`는 soft-delete 전후 정확한 수를 반환해야 합니다.
     *
     * 5개 삽입 → 초기 countActive=5, countDeleted=0
     * → 2개 softDelete → countActive=3, countDeleted=2
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countActive 와 countDeleted 는 soft-delete 전후 정확한 수를 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            val id1 = insertRecord("Alice")
            val id2 = insertRecord("Bob")
            insertRecord("Charlie")
            insertRecord("Dave")
            insertRecord("Eve")

            repository.countActive() shouldBeEqualTo 5L
            repository.countDeleted() shouldBeEqualTo 0L

            repository.softDeleteById(id1)
            repository.softDeleteById(id2)

            repository.countActive() shouldBeEqualTo 3L
            repository.countDeleted() shouldBeEqualTo 2L
        }
    }

    /**
     * `softDeleteAll`에 매칭되지 않는 predicate를 전달하면 영향 row 수로 0을 반환해야 합니다.
     *
     * 모든 row가 "active" category인데 "inactive"로 조건을 주면 아무것도 변경되지 않습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `softDeleteAll 은 매칭 없는 predicate 에서 0 을 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            insertRecord("Alice", "active")
            insertRecord("Bob", "active")

            val affected = repository.softDeleteAll { EdgeCaseTable.category eq "inactive" }
            affected shouldBeEqualTo 0

            // 원본 row 변경 없음
            repository.countActive() shouldBeEqualTo 2L
            repository.countDeleted() shouldBeEqualTo 0L
        }
    }

    /**
     * `softDeleteAll`은 predicate에 매칭되는 row 수를 반환해야 합니다.
     *
     * 4개 삽입 후 category="A" 조건으로 softDeleteAll 호출 시 해당 개수만큼 반환됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `softDeleteAll 은 영향받은 row 수를 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            insertRecord("Item1", "A")
            insertRecord("Item2", "A")
            insertRecord("Item3", "B")
            insertRecord("Item4", "B")

            val affected = repository.softDeleteAll { EdgeCaseTable.category eq "A" }
            affected shouldBeEqualTo 2

            repository.countActive() shouldBeEqualTo 2L
            repository.countDeleted() shouldBeEqualTo 2L
        }
    }

    /**
     * `restoreAll`은 soft-delete된 row를 복원하고 영향받은 row 수를 반환해야 합니다.
     *
     * 3개 삽입 → 전체 softDeleteAll → restoreAll → countActive=3 검증
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `restoreAll 은 삭제된 row 를 복원하고 영향 row 수 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            insertRecord("Alice")
            insertRecord("Bob")
            insertRecord("Charlie")

            // 전체 soft delete
            val deleted = repository.softDeleteAll()
            deleted shouldBeEqualTo 3

            repository.countActive() shouldBeEqualTo 0L
            repository.countDeleted() shouldBeEqualTo 3L

            // 전체 복원
            val restored = repository.restoreAll()
            restored shouldBeEqualTo 3

            repository.countActive() shouldBeEqualTo 3L
            repository.countDeleted() shouldBeEqualTo 0L
        }
    }

    /**
     * `restoreAll`에 매칭되지 않는 predicate를 전달하면 0을 반환해야 합니다.
     *
     * soft-delete된 row가 없는 상태에서 restoreAll을 호출하면 0이 반환됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `restoreAll 은 매칭 없는 predicate 에서 0 을 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            insertRecord("Alice", "active")
            insertRecord("Bob", "active")

            // 삭제된 row 없음 → restoreAll 호출
            val restored = repository.restoreAll { EdgeCaseTable.category eq "inactive" }
            restored shouldBeEqualTo 0

            repository.countActive() shouldBeEqualTo 2L
            repository.countDeleted() shouldBeEqualTo 0L
        }
    }

    /**
     * `findActivePage`는 soft-delete 필터와 페이징을 함께 적용해야 합니다.
     *
     * 6개 삽입 후 3개 softDelete → findActivePage(pageNumber=0, pageSize=10) → 3개만 반환
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findActivePage 는 soft-delete 필터 + 페이징 결합`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            val ids = (1..6).map { insertRecord("Item-$it") }

            // 처음 3개 soft delete
            ids.take(3).forEach { repository.softDeleteById(it) }

            val page = repository.findActivePage(pageNumber = 0, pageSize = 10)
            page.content shouldHaveSize 3
            page.totalCount shouldBeEqualTo 3L

            // 반환된 엔티티는 모두 isDeleted=false
            page.content.all { !it.isDeleted }.shouldBeTrue()
        }
    }

    /**
     * `findActivePage`는 pageSize보다 활성 엔티티 수가 적으면 실제 수만큼만 반환해야 합니다.
     *
     * 2개 활성 엔티티에 pageSize=10을 주면 content.size=2여야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findActivePage 는 활성 엔티티 수가 pageSize 보다 적으면 실제 수만큼 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            insertRecord("Alice")
            insertRecord("Bob")
            val deletedId = insertRecord("Charlie")
            repository.softDeleteById(deletedId)

            val page = repository.findActivePage(pageNumber = 0, pageSize = 10)
            page.content shouldHaveSize 2
            page.totalCount shouldBeEqualTo 2L
        }
    }

    /**
     * `findDeleted`는 soft-delete된 row만 반환해야 합니다.
     *
     * 4개 삽입 → 2개 softDelete → findDeleted() → 2개, isDeleted=true 검증
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findDeleted 는 삭제된 row 만 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            val id1 = insertRecord("Alice")
            val id2 = insertRecord("Bob")
            insertRecord("Charlie")
            insertRecord("Dave")

            repository.softDeleteById(id1)
            repository.softDeleteById(id2)

            val deleted = repository.findDeleted()
            deleted shouldHaveSize 2
            deleted.all { it.isDeleted }.shouldBeTrue()
            deleted.map { it.id }.toSet() shouldBeEqualTo setOf(id1, id2)
        }
    }

    /**
     * `findDeleted`는 추가 predicate를 조합하여 필터링할 수 있어야 합니다.
     *
     * category="A"로 삭제된 row만 반환하는 케이스를 검증합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findDeleted 는 추가 predicate 와 함께 soft-delete 필터를 적용`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            val idA1 = insertRecord("A-Item1", "A")
            val idA2 = insertRecord("A-Item2", "A")
            val idB = insertRecord("B-Item", "B")

            repository.softDeleteById(idA1)
            repository.softDeleteById(idA2)
            repository.softDeleteById(idB)

            // category="A"인 삭제된 row만 조회
            val deletedA = repository.findDeleted { EdgeCaseTable.category eq "A" }
            deletedA shouldHaveSize 2
            deletedA.all { it.isDeleted }.shouldBeTrue()
            deletedA.all { it.category == "A" }.shouldBeTrue()

            // category="B"인 삭제된 row만 조회
            val deletedB = repository.findDeleted { EdgeCaseTable.category eq "B" }
            deletedB shouldHaveSize 1
            deletedB.single().id shouldBeEqualTo idB
            deletedB.single().isDeleted.shouldBeTrue()
        }
    }

    /**
     * `countActive`에 predicate를 전달하면 조건을 함께 적용해야 합니다.
     *
     * category="A"인 활성 row 수만 반환하는지 검증합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countActive 는 추가 predicate 와 함께 동작`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            insertRecord("A1", "A")
            insertRecord("A2", "A")
            insertRecord("B1", "B")
            val deletedId = insertRecord("A3", "A")
            repository.softDeleteById(deletedId)

            repository.countActive { EdgeCaseTable.category eq "A" } shouldBeEqualTo 2L
            repository.countActive { EdgeCaseTable.category eq "B" } shouldBeEqualTo 1L
            repository.countDeleted { EdgeCaseTable.category eq "A" } shouldBeEqualTo 1L
        }
    }

    /**
     * `softDeleteAll`을 predicate 없이 호출하면 모든 row를 soft-delete해야 합니다.
     *
     * 전체 삭제 후 countActive=0, countDeleted=전체 count 를 검증합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `softDeleteAll 은 predicate 없이 호출 시 모든 row 를 soft-delete`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            repeat(4) { i -> insertRecord("Item-$i") }

            val affected = repository.softDeleteAll()
            affected shouldBeEqualTo 4

            repository.countActive() shouldBeEqualTo 0L
            repository.countDeleted() shouldBeEqualTo 4L

            val allRecords = repository.findAll()
            allRecords.all { it.isDeleted }.shouldBeTrue()
        }
    }

    /**
     * `findActive`는 soft-delete 되지 않은 row만 반환해야 합니다.
     *
     * findActive 후 반환된 모든 엔티티의 isDeleted=false를 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findActive 는 isDeleted false 인 row 만 반환`(testDB: TestDB) {
        withTables(testDB, EdgeCaseTable) {
            val activeId1 = insertRecord("Active1")
            val activeId2 = insertRecord("Active2")
            val deletedId = insertRecord("Deleted")

            repository.softDeleteById(deletedId)

            val actives = repository.findActive()
            actives shouldHaveSize 2
            actives.all { !it.isDeleted }.shouldBeTrue()
            actives.none { it.id == deletedId }.shouldBeTrue()

            val deleted = repository.findDeleted()
            deleted shouldHaveSize 1
            deleted.single().id shouldBeEqualTo deletedId
            deleted.single().isDeleted.shouldBeTrue()
        }
    }
}

package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseRecord
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseRepository
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseTable
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.withEdgeCaseTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [JdbcRepository] read 계열 메서드 엣지 케이스 테스트.
 *
 * `findByIdOrNull`, `findFirstOrNull`, `findLastOrNull`, `findAll`, `findWithFilters`,
 * `findBy`, `findByField`, `findByFieldOrNull`, `findAllByIds`, `findPage` 의
 * 경계 조건과 조합 동작을 검증한다.
 */
class JdbcRepositoryReadEdgeCaseTest : AbstractExposedTest() {

    companion object : KLogging()

    private val repo = EdgeCaseRepository

    // ── findByIdOrNull ──────────────────────────────────────────────────────

    /**
     * 존재하지 않는 id로 조회 시 null을 반환하는지 검증한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findByIdOrNull 은 존재하지 않는 id 에 null 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            repo.findByIdOrNull(999L).shouldBeNull()
        }
    }

    /**
     * 존재하는 id로 조회 시 올바른 엔티티를 반환하는지 검증한다.
     * name, age, isActive 값이 삽입한 값과 일치해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findByIdOrNull 은 존재하는 id 에 엔티티 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            val saved = repo.save(EdgeCaseRecord(name = "Alice", age = 30, isActive = true))
            val found = repo.findByIdOrNull(saved.id)
            found.shouldNotBeNull()
            found.name shouldBeEqualTo "Alice"
            found.age shouldBeEqualTo 30
            found.isActive.shouldBeTrue()
        }
    }

    // ── findFirstOrNull ─────────────────────────────────────────────────────

    /**
     * predicate + offset 조합으로 첫 번째 매칭 row를 올바르게 반환하는지 검증한다.
     * name이 "name-1"인 레코드를 삽입한 후 해당 조건으로 조회한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findFirstOrNull 은 predicate 조건으로 첫 번째 매칭 row 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            repo.save(EdgeCaseRecord(name = "name-0", age = 10))
            repo.save(EdgeCaseRecord(name = "name-1", age = 20))
            repo.save(EdgeCaseRecord(name = "name-2", age = 30))

            val found = repo.findFirstOrNull { EdgeCaseTable.name eq "name-1" }
            found.shouldNotBeNull()
            found.name shouldBeEqualTo "name-1"
            found.age shouldBeEqualTo 20
        }
    }

    // ── findLastOrNull ──────────────────────────────────────────────────────

    /**
     * PK 역순으로 조건에 맞는 마지막 row를 반환하는지 검증한다.
     * 같은 age를 가진 레코드 중 가장 높은 id를 가진 것을 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findLastOrNull 은 PK 역순 첫 매칭 row 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            repo.save(EdgeCaseRecord(name = "rec-1", age = 20))
            repo.save(EdgeCaseRecord(name = "rec-2", age = 20))
            repo.save(EdgeCaseRecord(name = "rec-3", age = 30))

            val found = repo.findLastOrNull { EdgeCaseTable.age eq 20 }
            found.shouldNotBeNull()
            // PK DESC → "rec-2"가 "rec-1"보다 id가 높으므로 "rec-2"가 반환되어야 한다
            found.name shouldBeEqualTo "rec-2"
        }
    }

    // ── findAll ─────────────────────────────────────────────────────────────

    /**
     * limit, offset, sortOrder 조합이 올바르게 동작하는지 검증한다.
     * 5개 삽입 후 DESC 정렬로 offset=1, limit=2 조회 시 올바른 2개가 반환되어야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll 은 limit offset sortOrder 조합 동작`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            (1..5).forEach { i ->
                repo.save(EdgeCaseRecord(name = "item-$i", age = i * 10))
            }

            // DESC 정렬: id가 큰 순서. offset=1이면 두 번째부터, limit=2이면 2개
            val result = repo.findAll(limit = 2, offset = 1L, sortOrder = SortOrder.DESC)
            result shouldHaveSize 2
        }
    }

    // ── findWithFilters ─────────────────────────────────────────────────────

    /**
     * vararg filter 들이 AND로 결합되는지 검증한다.
     * name="Alice" AND isActive=true 조건 모두 만족하는 row만 반환되어야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findWithFilters 는 vararg filter 들을 and 로 결합`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            repo.save(EdgeCaseRecord(name = "Alice", age = 25, isActive = true))
            repo.save(EdgeCaseRecord(name = "Bob", age = 30, isActive = true))
            repo.save(EdgeCaseRecord(name = "Alice-inactive", age = 25, isActive = false))

            val result = repo.findWithFilters(
                { EdgeCaseTable.name eq "Alice" },
                { EdgeCaseTable.isActive eq true },
            )
            result shouldHaveSize 1
            result.first().name shouldBeEqualTo "Alice"
        }
    }

    // ── findBy ──────────────────────────────────────────────────────────────

    /**
     * findBy 가 조건 매칭 row만 반환하는지 검증한다.
     * age > 18 조건으로 필터링 시 해당 row만 반환되어야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findBy 는 조건 매칭 row 만 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            repo.save(EdgeCaseRecord(name = "minor", age = 15))
            repo.save(EdgeCaseRecord(name = "adult1", age = 25))
            repo.save(EdgeCaseRecord(name = "adult2", age = 35))

            val predicate: () -> org.jetbrains.exposed.v1.core.Op<Boolean> =
                { EdgeCaseTable.age greater 18 }
            val result = repo.findBy(predicate)
            result shouldHaveSize 2
            result.map { it.name } shouldContainAll listOf("adult1", "adult2")
        }
    }

    // ── findByField ─────────────────────────────────────────────────────────

    /**
     * 같은 age를 가진 여러 row가 모두 반환되는지 검증한다.
     * findByField(table.age, 25) → 해당 age를 가진 2개가 반환되어야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findByField 는 컬럼 값 매칭 row 모두 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            repo.save(EdgeCaseRecord(name = "person-a", age = 25))
            repo.save(EdgeCaseRecord(name = "person-b", age = 25))
            repo.save(EdgeCaseRecord(name = "person-c", age = 30))

            val result = repo.findByField(EdgeCaseTable.age, 25)
            result shouldHaveSize 2
            result.map { it.age }.all { it == 25 }.shouldBeTrue()
        }
    }

    // ── findByFieldOrNull ───────────────────────────────────────────────────

    /**
     * 매칭 row가 없을 때 findByFieldOrNull이 null을 반환하는지 검증한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findByFieldOrNull 은 매칭 없으면 null 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            repo.save(EdgeCaseRecord(name = "existing", age = 20))

            val result = repo.findByFieldOrNull(EdgeCaseTable.name, "nonexistent")
            result.shouldBeNull()
        }
    }

    // ── findAllByIds ────────────────────────────────────────────────────────

    /**
     * inList로 지정한 id들만 정확히 반환되는지 검증한다.
     * 4개 삽입 후 2개 id만 조회 시 해당 2개만 반환되어야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAllByIds 는 inList 로 일괄 조회`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            val r1 = repo.save(EdgeCaseRecord(name = "batch-1", age = 10))
            val r2 = repo.save(EdgeCaseRecord(name = "batch-2", age = 20))
            repo.save(EdgeCaseRecord(name = "batch-3", age = 30))
            repo.save(EdgeCaseRecord(name = "batch-4", age = 40))

            val result = repo.findAllByIds(listOf(r1.id, r2.id))
            result shouldHaveSize 2
            result.map { it.name } shouldContainAll listOf("batch-1", "batch-2")
        }
    }

    // ── findPage ────────────────────────────────────────────────────────────

    /**
     * 빈 테이블에서 findPage 호출 시 content가 비어있고 totalCount=0인 페이지를 반환하는지 검증한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findPage 는 totalCount 0 시 빈 페이지 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            val page = repo.findPage(pageNumber = 0, pageSize = 10)
            page.content.shouldBeEmpty()
            page.totalCount shouldBeEqualTo 0L
            page.totalPages shouldBeEqualTo 0
            page.isFirst.shouldBeTrue()
            page.hasNext.shouldBeFalse()
        }
    }

    /**
     * 마지막 페이지 경계를 올바르게 반환하는지 검증한다.
     * 5개 삽입, pageSize=3, pageNumber=1 → 2개 반환, hasNext=false, isLast=true.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findPage 는 마지막 페이지 경계 검증`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            (1..5).forEach { i ->
                repo.save(EdgeCaseRecord(name = "page-item-$i", age = i))
            }

            val page = repo.findPage(pageNumber = 1, pageSize = 3)
            page.content shouldHaveSize 2
            page.totalCount shouldBeEqualTo 5L
            page.totalPages shouldBeEqualTo 2
            page.hasNext.shouldBeFalse()
            page.isLast.shouldBeTrue()
            page.hasPrevious.shouldBeTrue()
        }
    }
}

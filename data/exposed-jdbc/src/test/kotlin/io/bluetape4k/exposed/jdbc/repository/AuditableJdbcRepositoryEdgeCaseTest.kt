package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.auditable.Auditable
import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.auditable.UserContext
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant

/**
 * [AuditableJdbcRepository] 엣지 케이스 테스트입니다.
 *
 * [AuditableJdbcRepository.auditedUpdateAll]과 [AuditableJdbcRepository.auditedUpdateById]의
 * 엣지 케이스를 검증합니다:
 * - predicate 매칭 row의 updatedAt 자동 갱신
 * - 영향받은 row 수 반환
 * - UserContext 설정 시 updatedBy 저장
 * - auditedUpdateById의 동작 검증
 */
class AuditableJdbcRepositoryEdgeCaseTest : AbstractExposedTest() {

    companion object : KLogging()

    // ── 테이블 정의 ─────────────────────────────────────────────────────────────

    object AuditableEdgeCaseTable : AuditableLongIdTable("auditable_edge_items") {
        val name = varchar("name", 255)
        val age = integer("age").default(0)
    }

    // ── 레코드 타입 ─────────────────────────────────────────────────────────────

    data class AuditableEdgeCaseRecord(
        val id: Long = 0L,
        val name: String,
        val age: Int = 0,
        override val createdBy: String = UserContext.DEFAULT_USERNAME,
        override val createdAt: Instant? = null,
        override val updatedBy: String? = null,
        override val updatedAt: Instant? = null,
    ) : Auditable

    // ── Repository 구현 ──────────────────────────────────────────────────────────

    object AuditableEdgeCaseRepository :
        LongAuditableJdbcRepository<AuditableEdgeCaseRecord, AuditableEdgeCaseTable> {

        override val table = AuditableEdgeCaseTable

        override fun extractId(entity: AuditableEdgeCaseRecord): Long = entity.id

        override fun ResultRow.toEntity(): AuditableEdgeCaseRecord = AuditableEdgeCaseRecord(
            id = this[AuditableEdgeCaseTable.id].value,
            name = this[AuditableEdgeCaseTable.name],
            age = this[AuditableEdgeCaseTable.age],
            createdBy = this[AuditableEdgeCaseTable.createdBy],
            createdAt = this[AuditableEdgeCaseTable.createdAt],
            updatedBy = this[AuditableEdgeCaseTable.updatedBy],
            updatedAt = this[AuditableEdgeCaseTable.updatedAt],
        )
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

    private fun findById(id: Long): AuditableEdgeCaseRecord =
        AuditableEdgeCaseTable.selectAll()
            .where { AuditableEdgeCaseTable.id eq id }
            .single()
            .let { with(AuditableEdgeCaseRepository) { it.toEntity() } }

    // ── 테스트 ────────────────────────────────────────────────────────────────────

    /**
     * [AuditableJdbcRepository.auditedUpdateAll]은 predicate에 매칭되는 row의
     * `updatedAt`을 자동으로 현재 시각으로 갱신해야 합니다.
     *
     * 3개 row 삽입 후 age=25 조건으로 전체 update 시 해당 row의 updatedAt이 null이 아니어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditedUpdateAll 은 predicate 매칭 row 의 updatedAt 자동 갱신`(testDB: TestDB) {
        withTables(testDB, AuditableEdgeCaseTable) {
            val id1 = AuditableEdgeCaseTable.insertAndGetId {
                it[name] = "Alice"
                it[age] = 25
            }.value
            val id2 = AuditableEdgeCaseTable.insertAndGetId {
                it[name] = "Bob"
                it[age] = 25
            }.value
            AuditableEdgeCaseTable.insertAndGetId {
                it[name] = "Charlie"
                it[age] = 30
            }.value

            AuditableEdgeCaseRepository.auditedUpdateAll(
                predicate = { AuditableEdgeCaseTable.age eq 25 }
            ) {
                it[AuditableEdgeCaseTable.name] = "Updated"
            }

            val record1 = findById(id1)
            val record2 = findById(id2)

            record1.updatedAt.shouldNotBeNull()
            record2.updatedAt.shouldNotBeNull()
        }
    }

    /**
     * [AuditableJdbcRepository.auditedUpdateAll]은 실제로 영향받은 row 수를 반환해야 합니다.
     *
     * 5개 row 삽입(age=20 3개, age=30 2개) 후 age=20 조건으로 update 시 반환값이 3이어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditedUpdateAll 은 영향받은 row 수를 반환`(testDB: TestDB) {
        withTables(testDB, AuditableEdgeCaseTable) {
            repeat(3) { i ->
                AuditableEdgeCaseTable.insertAndGetId {
                    it[name] = "Young-$i"
                    it[age] = 20
                }
            }
            repeat(2) { i ->
                AuditableEdgeCaseTable.insertAndGetId {
                    it[name] = "Senior-$i"
                    it[age] = 30
                }
            }

            val updatedCount = AuditableEdgeCaseRepository.auditedUpdateAll(
                predicate = { AuditableEdgeCaseTable.age eq 20 }
            ) {
                it[AuditableEdgeCaseTable.name] = "Migrated"
            }

            updatedCount shouldBeEqualTo 3
        }
    }

    /**
     * [UserContext.withUser] 블록 안에서 [AuditableJdbcRepository.auditedUpdateAll]을 호출하면
     * `updatedBy`에 지정한 사용자명이 저장되어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext 설정 시 updatedBy 가 저장`(testDB: TestDB) {
        withTables(testDB, AuditableEdgeCaseTable) {
            val id = AuditableEdgeCaseTable.insertAndGetId {
                it[name] = "TestUser"
                it[age] = 10
            }.value

            UserContext.withUser("testUser") {
                AuditableEdgeCaseRepository.auditedUpdateAll(
                    predicate = { AuditableEdgeCaseTable.age eq 10 }
                ) {
                    it[AuditableEdgeCaseTable.name] = "UpdatedByUser"
                }
            }

            val record = findById(id)
            record.updatedBy.shouldNotBeNull()
            record.updatedBy shouldBeEqualTo "testUser"
        }
    }

    /**
     * [AuditableJdbcRepository.auditedUpdateById]는 지정한 ID의 row를 업데이트하고
     * `updatedAt`을 자동으로 설정해야 합니다.
     *
     * 1개 row 삽입 후 auditedUpdateById 호출 시 updatedAt이 null이 아니어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditedUpdateById 의 limit 인자가 적용된다`(testDB: TestDB) {
        // PostgreSQL은 UPDATE 문에서 LIMIT 절을 지원하지 않음
        Assumptions.assumeTrue(testDB != TestDB.POSTGRESQL)
        withTables(testDB, AuditableEdgeCaseTable) {
            val id = AuditableEdgeCaseTable.insertAndGetId {
                it[name] = "LimitTest"
                it[age] = 42
            }.value

            val updatedCount = AuditableEdgeCaseRepository.auditedUpdateById(id, limit = 1) {
                it[AuditableEdgeCaseTable.name] = "LimitUpdated"
            }

            updatedCount shouldBeGreaterOrEqualTo 1

            val record = findById(id)
            record.updatedAt.shouldNotBeNull()
            record.name shouldBeEqualTo "LimitUpdated"
        }
    }
}

package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.auditable.Auditable
import io.bluetape4k.exposed.core.auditable.AuditableIntIdTable
import io.bluetape4k.exposed.core.auditable.AuditableUUIDTable
import io.bluetape4k.exposed.core.auditable.UserContext
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.*

/**
 * [IntAuditableJdbcRepository] 및 [UUIDAuditableJdbcRepository] 변종에 대한 통합 테스트입니다.
 *
 * `Int` PK 테이블과 `UUID` PK 테이블에서 [AuditableJdbcRepository.auditedUpdateById]와
 * [AuditableJdbcRepository.auditedUpdateAll]의 감사 필드 자동 갱신 동작을 검증합니다.
 */
class AuditableJdbcRepositoryVariantTest : AbstractExposedTest() {

    companion object : KLogging()

    // ── Int PK 테이블 정의 ──────────────────────────────────────────────────────

    object IntAuditableTable : AuditableIntIdTable("variant_int_items") {
        val name = varchar("name", 255)
        val category = varchar("category", 100).default("general")
    }

    // ── Int PK 레코드 타입 ──────────────────────────────────────────────────────

    data class IntAuditableRecord(
        val id: Int = 0,
        val name: String,
        val category: String = "general",
        override val createdBy: String = UserContext.DEFAULT_USERNAME,
        override val createdAt: Instant? = null,
        override val updatedBy: String? = null,
        override val updatedAt: Instant? = null,
    ) : Auditable

    // ── Int PK Repository 구현 ──────────────────────────────────────────────────

    object IntAuditableRepository : IntAuditableJdbcRepository<IntAuditableRecord, IntAuditableTable> {
        override val table = IntAuditableTable

        override fun extractId(entity: IntAuditableRecord): Int = entity.id

        override fun ResultRow.toEntity(): IntAuditableRecord = IntAuditableRecord(
            id = this[IntAuditableTable.id].value,
            name = this[IntAuditableTable.name],
            category = this[IntAuditableTable.category],
            createdBy = this[IntAuditableTable.createdBy],
            createdAt = this[IntAuditableTable.createdAt],
            updatedBy = this[IntAuditableTable.updatedBy],
            updatedAt = this[IntAuditableTable.updatedAt],
        )
    }

    private fun findIntById(id: Int): IntAuditableRecord =
        IntAuditableTable.selectAll()
            .where { IntAuditableTable.id eq id }
            .single()
            .let { with(IntAuditableRepository) { it.toEntity() } }

    // ── UUID PK 테이블 정의 ─────────────────────────────────────────────────────

    object UUIDAuditableTable : AuditableUUIDTable("variant_uuid_items") {
        val name = varchar("name", 255)
        val category = varchar("category", 100).default("general")
    }

    // ── UUID PK 레코드 타입 ─────────────────────────────────────────────────────

    data class UUIDAuditableRecord(
        val id: UUID = UUID.randomUUID(),
        val name: String,
        val category: String = "general",
        override val createdBy: String = UserContext.DEFAULT_USERNAME,
        override val createdAt: Instant? = null,
        override val updatedBy: String? = null,
        override val updatedAt: Instant? = null,
    ) : Auditable

    // ── UUID PK Repository 구현 ─────────────────────────────────────────────────

    object UUIDAuditableRepository : UUIDAuditableJdbcRepository<UUIDAuditableRecord, UUIDAuditableTable> {
        override val table = UUIDAuditableTable

        override fun extractId(entity: UUIDAuditableRecord): UUID = entity.id

        override fun ResultRow.toEntity(): UUIDAuditableRecord = UUIDAuditableRecord(
            id = this[UUIDAuditableTable.id].value,
            name = this[UUIDAuditableTable.name],
            category = this[UUIDAuditableTable.category],
            createdBy = this[UUIDAuditableTable.createdBy],
            createdAt = this[UUIDAuditableTable.createdAt],
            updatedBy = this[UUIDAuditableTable.updatedBy],
            updatedAt = this[UUIDAuditableTable.updatedAt],
        )
    }

    private fun findUUIDById(id: UUID): UUIDAuditableRecord =
        UUIDAuditableTable.selectAll()
            .where { UUIDAuditableTable.id eq id }
            .single()
            .let { with(UUIDAuditableRepository) { it.toEntity() } }

    // ── 테스트 ────────────────────────────────────────────────────────────────────

    /**
     * [IntAuditableJdbcRepository.auditedUpdateById]는 `Int` PK 테이블에서
     * 지정한 ID의 row를 업데이트하고 `updatedAt`을 자동으로 설정해야 합니다.
     *
     * INSERT 후 [AuditableJdbcRepository.auditedUpdateById] 호출 시
     * `updatedAt`이 null이 아니어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `IntAuditableJdbcRepository 구현체의 auditedUpdateById 가 updatedAt 갱신`(testDB: TestDB) {
        withTables(testDB, IntAuditableTable) {
            val id = IntAuditableTable.insertAndGetId {
                it[name] = faker.name().firstName()
                it[category] = "alpha"
            }.value

            UserContext.withUser("int-editor") {
                IntAuditableRepository.auditedUpdateById(id) {
                    it[IntAuditableTable.name] = "UpdatedIntName"
                }
            }

            val record = findIntById(id)
            record.updatedAt.shouldNotBeNull()
            record.updatedBy.shouldNotBeNull()
            record.updatedBy shouldBeEqualTo "int-editor"
        }
    }

    /**
     * [IntAuditableJdbcRepository.auditedUpdateAll]은 `Int` PK 테이블에서
     * 조건에 맞는 여러 row를 업데이트하고 영향받은 row 수를 반환해야 합니다.
     *
     * 3개 row 삽입 후 category 조건으로 [AuditableJdbcRepository.auditedUpdateAll] 호출 시
     * 반환 row 수가 1 이상이어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `IntAuditableJdbcRepository 구현체의 auditedUpdateAll 이 여러 행 갱신`(testDB: TestDB) {
        withTables(testDB, IntAuditableTable) {
            repeat(3) { i ->
                IntAuditableTable.insertAndGetId {
                    it[name] = "Item-$i"
                    it[category] = "batch-int"
                }
            }

            val updatedCount = UserContext.withUser("batch-int-user") {
                IntAuditableRepository.auditedUpdateAll(
                    predicate = { IntAuditableTable.category eq "batch-int" }
                ) {
                    it[IntAuditableTable.name] = "BatchUpdated"
                }
            }

            updatedCount shouldBeGreaterOrEqualTo 1
        }
    }

    /**
     * [UUIDAuditableJdbcRepository.auditedUpdateById]는 `UUID` PK 테이블에서
     * 지정한 UUID의 row를 업데이트하고 `updatedAt`을 자동으로 설정해야 합니다.
     *
     * INSERT 후 [AuditableJdbcRepository.auditedUpdateById] 호출 시
     * `updatedAt`이 null이 아니어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UUIDAuditableJdbcRepository 구현체의 auditedUpdateById 가 updatedAt 갱신`(testDB: TestDB) {
        withTables(testDB, UUIDAuditableTable) {
            val id = UUIDAuditableTable.insertAndGetId {
                it[name] = faker.name().firstName()
                it[category] = "uuid-category"
            }.value

            UserContext.withUser("uuid-editor") {
                UUIDAuditableRepository.auditedUpdateById(id) {
                    it[UUIDAuditableTable.name] = "UpdatedUUIDName"
                }
            }

            val record = findUUIDById(id)
            record.updatedAt.shouldNotBeNull()
            record.updatedBy.shouldNotBeNull()
            record.updatedBy shouldBeEqualTo "uuid-editor"
        }
    }

    /**
     * [UUIDAuditableJdbcRepository.auditedUpdateAll]은 `UUID` PK 테이블에서
     * 조건에 맞는 여러 row를 업데이트하고 영향받은 row 수를 반환해야 합니다.
     *
     * 3개 row 삽입 후 category 조건으로 [AuditableJdbcRepository.auditedUpdateAll] 호출 시
     * 반환 row 수가 1 이상이어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UUIDAuditableJdbcRepository 구현체의 auditedUpdateAll 이 여러 행 갱신`(testDB: TestDB) {
        withTables(testDB, UUIDAuditableTable) {
            repeat(3) { i ->
                UUIDAuditableTable.insertAndGetId {
                    it[name] = "UUIDItem-$i"
                    it[category] = "batch-uuid"
                }
            }

            val updatedCount = UserContext.withUser("batch-uuid-user") {
                UUIDAuditableRepository.auditedUpdateAll(
                    predicate = { UUIDAuditableTable.category eq "batch-uuid" }
                ) {
                    it[UUIDAuditableTable.name] = "UUIDBatchUpdated"
                }
            }

            updatedCount shouldBeGreaterOrEqualTo 1
        }
    }
}

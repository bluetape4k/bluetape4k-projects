package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.auditable.Auditable
import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.auditable.UserContext
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant

/**
 * [AuditableJdbcRepository] 통합 테스트입니다.
 *
 * INSERT 시 감사 필드 자동 설정, [UserContext.withUser] 컨텍스트 전파,
 * [AuditableJdbcRepository.auditedUpdateById] 및 [AuditableJdbcRepository.auditedUpdateAll] 동작을 검증합니다.
 */
class AuditableJdbcRepositoryTest: AbstractExposedTest() {

    // 테이블 정의
    object ActorTable: AuditableLongIdTable("auditable_actors") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
    }

    // Record (data class) — Auditable 구현 포함
    data class ActorRecord(
        val firstName: String,
        val lastName: String,
        override val createdBy: String = UserContext.DEFAULT_USERNAME,
        override val createdAt: Instant? = null,
        override val updatedBy: String? = null,
        override val updatedAt: Instant? = null,
        val id: Long = 0L,
    ): Auditable

    // Repository 구현
    object ActorRepository: LongAuditableJdbcRepository<ActorRecord, ActorTable> {
        override val table = ActorTable

        override fun extractId(entity: ActorRecord) = entity.id

        override fun ResultRow.toEntity(): ActorRecord = ActorRecord(
            firstName = this[ActorTable.firstName],
            lastName = this[ActorTable.lastName],
            createdBy = this[ActorTable.createdBy],
            createdAt = this[ActorTable.createdAt],
            updatedBy = this[ActorTable.updatedBy],
            updatedAt = this[ActorTable.updatedAt],
            id = this[ActorTable.id].value,
        )
    }

    private fun findById(id: Long): ActorRecord =
        ActorTable.selectAll()
            .where { ActorTable.id eq id }
            .single()
            .let { with(ActorRepository) { it.toEntity() } }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `INSERT 후 createdBy는 system이고 createdAt은 null이 아니다`(testDB: TestDB) {
        withTables(testDB, ActorTable) {
            val id = ActorTable.insertAndGetId {
                it[firstName] = faker.name().firstName()
                it[lastName] = faker.name().lastName()
            }.value

            val actor = findById(id)
            actor.createdBy shouldBeEqualTo UserContext.DEFAULT_USERNAME
            actor.createdAt.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `INSERT 직후 updatedAt과 updatedBy는 null이다`(testDB: TestDB) {
        withTables(testDB, ActorTable) {
            val id = ActorTable.insertAndGetId {
                it[firstName] = faker.name().firstName()
                it[lastName] = faker.name().lastName()
            }.value

            val actor = findById(id)
            actor.updatedAt.shouldBeNull()
            actor.updatedBy.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `UserContext withUser 내 INSERT 시 createdBy에 지정 사용자명이 설정된다`(testDB: TestDB) {
        withTables(testDB, ActorTable) {
            val id = UserContext.withUser("admin") {
                ActorTable.insertAndGetId {
                    it[firstName] = faker.name().firstName()
                    it[lastName] = faker.name().lastName()
                }.value
            }

            val actor = findById(id)
            actor.createdBy shouldBeEqualTo "admin"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditedUpdateById 후 updatedBy와 updatedAt이 설정된다`(testDB: TestDB) {
        withTables(testDB, ActorTable) {
            val id = ActorTable.insertAndGetId {
                it[firstName] = faker.name().firstName()
                it[lastName] = faker.name().lastName()
            }.value

            UserContext.withUser("editor") {
                ActorRepository.auditedUpdateById(id) {
                    it[ActorTable.firstName] = "UpdatedFirst"
                }
            }

            val actor = findById(id)
            actor.updatedBy.shouldNotBeNull()
            actor.updatedBy shouldBeEqualTo "editor"
            actor.updatedAt.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditedUpdateAll 후 모든 레코드의 updatedAt이 설정된다`(testDB: TestDB) {
        withTables(testDB, ActorTable) {
            val id1 = ActorTable.insertAndGetId {
                it[firstName] = faker.name().firstName()
                it[lastName] = "Smith"
            }.value
            val id2 = ActorTable.insertAndGetId {
                it[firstName] = faker.name().firstName()
                it[lastName] = "Smith"
            }.value

            UserContext.withUser("batch") {
                ActorRepository.auditedUpdateAll(predicate = { ActorTable.lastName eq "Smith" }) {
                    it[ActorTable.firstName] = "Migrated"
                }
            }

            val actor1 = findById(id1)
            val actor2 = findById(id2)
            actor1.updatedAt.shouldNotBeNull()
            actor2.updatedAt.shouldNotBeNull()
            actor1.updatedBy shouldBeEqualTo "batch"
            actor2.updatedBy shouldBeEqualTo "batch"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `일반 updateById 사용 시 감사 필드는 변경되지 않는다`(testDB: TestDB) {
        withTables(testDB, ActorTable) {
            val id = ActorTable.insertAndGetId {
                it[firstName] = faker.name().firstName()
                it[lastName] = faker.name().lastName()
            }.value

            // 일반 updateById는 감사 필드 미변경
            ActorRepository.updateById(id) {
                it[ActorTable.firstName] = "DirectUpdate"
            }

            val actor = findById(id)
            actor.updatedAt.shouldBeNull()
            actor.updatedBy.shouldBeNull()
        }
    }
}

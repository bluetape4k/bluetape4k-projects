package io.bluetape4k.exposed.lettuce.domain

import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.lettuce.repository.AbstractJdbcLettuceRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

/**
 * [AbstractJdbcLettuceRepository] 구현체 — 테스트용 UserCredentialsRecord 레포지토리.
 *
 * Client-generated UUID ID ([UserCredentialsTable]) 를 사용하는 테스트 레포지토리.
 */
class UserCredentialRepository(
    client: RedisClient,
    config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
) : AbstractJdbcLettuceRepository<UUID, UserCredentialsRecord>(client, config) {
    companion object : KLogging()

    override val table: IdTable<UUID> = UserCredentialsTable

    override fun ResultRow.toEntity(): UserCredentialsRecord =
        UserCredentialsRecord(
            id = this[UserCredentialsTable.id].value,
            loginId = this[UserCredentialsTable.loginId],
            email = this[UserCredentialsTable.email],
            lastLoginAt = this[UserCredentialsTable.lastLoginAt],
            createdAt = this[UserCredentialsTable.createdAt]
        )

    override fun UpdateStatement.updateEntity(entity: UserCredentialsRecord) {
        this[UserCredentialsTable.loginId] = entity.loginId
        this[UserCredentialsTable.email] = entity.email
        this[UserCredentialsTable.lastLoginAt] = entity.lastLoginAt
    }

    override fun BatchInsertStatement.insertEntity(entity: UserCredentialsRecord) {
        this[UserCredentialsTable.id] = entity.id
        this[UserCredentialsTable.loginId] = entity.loginId
        this[UserCredentialsTable.email] = entity.email
        this[UserCredentialsTable.lastLoginAt] = entity.lastLoginAt
    }

    override fun extractId(entity: UserCredentialsRecord): UUID = entity.id

    /** DB에 직접 row를 삽입하고 UserCredentialsRecord를 반환한다 (테스트 편의용). */
    fun createInDb(record: UserCredentialsRecord): UserCredentialsRecord =
        transaction {
            UserCredentialsTable.insertAndGetId {
                it[UserCredentialsTable.id] = record.id
                it[UserCredentialsTable.loginId] = record.loginId
                it[UserCredentialsTable.email] = record.email
                it[UserCredentialsTable.lastLoginAt] = record.lastLoginAt
            }
            record
        }

    /** DB에서 직접 조회한다 (캐시를 거치지 않음, 테스트 검증용). */
    fun findFromDb(id: UUID): UserCredentialsRecord? =
        transaction {
            UserCredentialsTable
                .selectAll()
                .where { UserCredentialsTable.id eq id }
                .singleOrNull()
                ?.let {
                    UserCredentialsRecord(
                        id = it[UserCredentialsTable.id].value,
                        loginId = it[UserCredentialsTable.loginId],
                        email = it[UserCredentialsTable.email],
                        lastLoginAt = it[UserCredentialsTable.lastLoginAt],
                        createdAt = it[UserCredentialsTable.createdAt]
                    )
                }
        }

    /** DB의 전체 레코드 수를 반환한다 (테스트 검증용). */
    override fun countFromDb(): Long =
        transaction {
            UserCredentialsTable.selectAll().count()
        }
}

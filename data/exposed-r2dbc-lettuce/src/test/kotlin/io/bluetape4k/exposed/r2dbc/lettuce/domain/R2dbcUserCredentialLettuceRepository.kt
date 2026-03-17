package io.bluetape4k.exposed.r2dbc.lettuce.domain

import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.toUserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.lettuce.repository.AbstractR2dbcLettuceRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import java.util.UUID

/**
 * Client-generated UUID PK를 가진 [UserCredentialsRecord] 레포지토리 구체 구현체.
 *
 * [UserCredentialsTable]은 auto-increment 없이 클라이언트가 UUID를 직접 제공하는 테이블이다.
 * [insertEntity]에서 `id` 컬럼을 명시적으로 설정한다.
 */
class R2dbcUserCredentialLettuceRepository(
    client: RedisClient,
    config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
): AbstractR2dbcLettuceRepository<UUID, UserCredentialsRecord>(client, config) {
    companion object: KLoggingChannel()

    override val table: IdTable<UUID> = UserCredentialsTable

    override suspend fun ResultRow.toEntity(): UserCredentialsRecord = toUserCredentialsRecord()

    override fun UpdateStatement.updateEntity(entity: UserCredentialsRecord) {
        this[UserCredentialsTable.loginId] = entity.loginId
        this[UserCredentialsTable.email] = entity.email
        this[UserCredentialsTable.lastLoginAt] = entity.lastLoginAt
        // updatedAt은 테스트에서 별도로 관리 — 자동 설정하지 않음
    }

    override fun BatchInsertStatement.insertEntity(entity: UserCredentialsRecord) {
        // Client-generated UUID: id 컬럼을 명시적으로 설정
        this[UserCredentialsTable.id] = entity.id
        this[UserCredentialsTable.loginId] = entity.loginId
        this[UserCredentialsTable.email] = entity.email
        this[UserCredentialsTable.lastLoginAt] = entity.lastLoginAt
    }

    override fun extractId(entity: UserCredentialsRecord): UUID = entity.id
}

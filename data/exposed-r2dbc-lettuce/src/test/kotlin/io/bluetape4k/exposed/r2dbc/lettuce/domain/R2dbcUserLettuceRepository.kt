package io.bluetape4k.exposed.r2dbc.lettuce.domain

import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.toUserRecord
import io.bluetape4k.exposed.r2dbc.lettuce.repository.AbstractR2dbcLettuceRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement

/**
 * R2DBC Lettuce 통합 테스트용 [UserRecord] 레포지토리 구체 구현체.
 */
class R2dbcUserLettuceRepository(
    client: RedisClient,
    config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
): AbstractR2dbcLettuceRepository<Long, UserRecord>(client, config) {
    companion object: KLoggingChannel()

    override val table: IdTable<Long> = UserTable

    override suspend fun ResultRow.toEntity(): UserRecord = toUserRecord()

    override fun UpdateStatement.updateEntity(entity: UserRecord) {
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.email] = entity.email
        // updatedAt은 테스트에서 별도로 관리 — 자동 설정하지 않음
    }

    override fun BatchInsertStatement.insertEntity(entity: UserRecord) {
        // AutoIncrement ID 테이블이므로 id 컬럼은 DB가 생성
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.email] = entity.email
    }

    override fun extractId(entity: UserRecord): Long = entity.id
}

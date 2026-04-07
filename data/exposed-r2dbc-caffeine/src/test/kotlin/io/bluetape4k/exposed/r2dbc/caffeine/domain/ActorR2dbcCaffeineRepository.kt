package io.bluetape4k.exposed.r2dbc.caffeine.domain

import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.toActorRecord
import io.bluetape4k.exposed.r2dbc.caffeine.repository.AbstractR2dbcCaffeineRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement

/**
 * R2DBC Caffeine 통합 테스트용 [ActorRecord] 레포지토리 구체 구현체.
 */
class ActorR2dbcCaffeineRepository(
    config: LocalCacheConfig = LocalCacheConfig.WRITE_THROUGH,
): AbstractR2dbcCaffeineRepository<Long, ActorRecord>(config) {

    companion object: KLoggingChannel()

    override val table: IdTable<Long> = ActorTable

    override suspend fun ResultRow.toEntity(): ActorRecord = toActorRecord()

    override fun UpdateStatement.updateEntity(entity: ActorRecord) {
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
        this[ActorTable.email] = entity.email
    }

    override fun BatchInsertStatement.insertEntity(entity: ActorRecord) {
        // AutoIncrement ID 테이블이므로 id 컬럼은 DB가 생성
        this[ActorTable.firstName] = entity.firstName
        this[ActorTable.lastName] = entity.lastName
        this[ActorTable.email] = entity.email
    }

    override fun extractId(entity: ActorRecord): Long = entity.id
}

package io.bluetape4k.exposed.r2dbc.caffeine.domain

import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.CredentialRecord
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.CredentialTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.toCredentialRecord
import io.bluetape4k.exposed.r2dbc.caffeine.repository.AbstractR2dbcCaffeineRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import java.util.*

/**
 * R2DBC Caffeine 통합 테스트용 [CredentialRecord] 레포지토리 구체 구현체.
 *
 * Client-generated UUID ID ([CredentialTable])를 사용합니다.
 */
class CredentialR2dbcCaffeineRepository(
    config: LocalCacheConfig = LocalCacheConfig.WRITE_THROUGH,
): AbstractR2dbcCaffeineRepository<UUID, CredentialRecord>(config) {

    companion object: KLoggingChannel()

    override val table: IdTable<UUID> = CredentialTable

    override suspend fun ResultRow.toEntity(): CredentialRecord = toCredentialRecord()

    override fun UpdateStatement.updateEntity(entity: CredentialRecord) {
        this[CredentialTable.loginId] = entity.loginId
        this[CredentialTable.email] = entity.email
        this[CredentialTable.lastLoginAt] = entity.lastLoginAt
    }

    override fun BatchInsertStatement.insertEntity(entity: CredentialRecord) {
        this[CredentialTable.id] = entity.id
        this[CredentialTable.loginId] = entity.loginId
        this[CredentialTable.email] = entity.email
        this[CredentialTable.lastLoginAt] = entity.lastLoginAt
    }

    override fun extractId(entity: CredentialRecord): UUID = entity.id
}

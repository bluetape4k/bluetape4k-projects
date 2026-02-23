package io.bluetape4k.exposed.r2dbc.hazelcast.repository

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.exposed.r2dbc.hazelcast.repository.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.hazelcast.repository.UserSchema.toUserRecord
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * Hazelcast R2DBC 캐시 Repository 테스트용 사용자 Repository 구현체입니다.
 *
 * @param hazelcastInstance Hazelcast 인스턴스 (클라이언트 또는 임베디드)
 * @param cacheName IMap 이름 (캐시 이름)
 */
class UserHazelcastR2dbcCacheRepository(
    hazelcastInstance: HazelcastInstance,
    cacheName: String = "r2dbc:hazelcast:users",
): AbstractHazelcastR2dbcCacheRepository<UserSchema.UserRecord, Long>(hazelcastInstance, cacheName) {

    override val entityTable: IdTable<Long> = UserTable

    override suspend fun ResultRow.toEntity(): UserSchema.UserRecord = toUserRecord()
}

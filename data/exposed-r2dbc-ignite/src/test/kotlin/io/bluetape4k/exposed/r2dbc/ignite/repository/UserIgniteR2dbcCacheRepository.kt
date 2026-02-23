package io.bluetape4k.exposed.r2dbc.ignite.repository

import io.bluetape4k.exposed.r2dbc.ignite.repository.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.ignite.repository.UserSchema.toUserRecord
import io.bluetape4k.ignite.cache.IgniteNearCache
import io.bluetape4k.ignite.cache.IgniteNearCacheConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.apache.ignite.client.IgniteClient
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * Ignite 2.x 씬 클라이언트 + Exposed R2DBC 기반 사용자 캐시 Repository 구현체입니다.
 *
 * Caffeine(Front Cache) + Ignite 2.x [org.apache.ignite.client.ClientCache](Back Cache) 구조입니다.
 *
 * @param igniteClient Ignite 2.x 씬 클라이언트
 * @param config NearCache 설정 (cacheName = Ignite 2.x 캐시 이름)
 */
class UserIgniteR2dbcCacheRepository(
    igniteClient: IgniteClient,
    config: IgniteNearCacheConfig = IgniteNearCacheConfig(cacheName = "TEST_USERS"),
): AbstractIgniteR2dbcCacheRepository<UserRecord, Long>(igniteClient, config) {

    companion object: KLoggingChannel()

    override val entityTable = UserSchema.UserTable

    override suspend fun ResultRow.toEntity(): UserRecord = toUserRecord()

    /**
     * Ignite 2.x [IgniteNearCache] 인스턴스를 생성합니다.
     * Caffeine(Front) + ClientCache(Back) 2-Tier 구조입니다.
     */
    override fun createNearCache(): IgniteNearCache<Long, UserRecord> =
        IgniteNearCache(igniteClient, config)
}

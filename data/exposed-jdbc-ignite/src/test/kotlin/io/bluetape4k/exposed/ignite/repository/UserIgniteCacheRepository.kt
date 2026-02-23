package io.bluetape4k.exposed.ignite.repository

import io.bluetape4k.exposed.ignite.repository.UserSchema.UserRecord
import io.bluetape4k.exposed.ignite.repository.UserSchema.toUserRecord
import io.bluetape4k.ignite.cache.IgniteClientNearCache
import io.bluetape4k.ignite.cache.IgniteNearCache
import io.bluetape4k.ignite.cache.IgniteNearCacheConfig
import io.bluetape4k.logging.KLogging
import org.apache.ignite.client.IgniteClient
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * Ignite 2.x 씬 클라이언트 기반 사용자 캐시 Repository 구현체입니다.
 *
 * 테스트 전용 구현으로, [UserRecord]를 Ignite 2.x NearCache에 저장합니다.
 *
 * @param igniteClient Ignite 2.x 씬 클라이언트
 * @param config NearCache 설정 (기본: cacheName = "test-users")
 */
class UserIgniteCacheRepository(
    igniteClient: IgniteClient,
    config: IgniteNearCacheConfig = IgniteNearCacheConfig(cacheName = "test-users"),
): AbstractIgniteCacheRepository<UserRecord, Long>(igniteClient, config) {

    companion object: KLogging()

    override val entityTable = UserSchema.UserTable

    override fun ResultRow.toEntity(): UserRecord = toUserRecord()

    /**
     * Ignite 2.x 씬 클라이언트 기반 [IgniteNearCache]를 생성합니다.
     * Caffeine(Front) + Ignite ClientCache(Back) 2-Tier 구조입니다.
     */
    override fun createNearCache(): IgniteNearCache<Long, UserRecord> =
        IgniteClientNearCache(igniteClient, config)
}

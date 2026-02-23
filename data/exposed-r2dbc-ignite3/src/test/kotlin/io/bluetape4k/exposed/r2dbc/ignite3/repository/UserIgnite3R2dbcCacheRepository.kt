package io.bluetape4k.exposed.r2dbc.ignite3.repository

import io.bluetape4k.exposed.r2dbc.ignite3.repository.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.ignite3.repository.UserSchema.toUserRecord
import io.bluetape4k.ignite3.cache.IgniteNearCache
import io.bluetape4k.ignite3.cache.IgniteNearCacheConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.apache.ignite.client.IgniteClient
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * Ignite 3.x 씬 클라이언트 + Exposed R2DBC 기반 사용자 캐시 Repository 구현체입니다.
 *
 * Caffeine(Front Cache) + Ignite 3.x [org.apache.ignite.table.KeyValueView](Back Cache) 구조입니다.
 *
 * **주의**: 사용 전 Ignite 3.x 클러스터에 테이블이 생성되어 있어야 합니다.
 * ```sql
 * CREATE TABLE IF NOT EXISTS TEST_USERS (
 *     ID BIGINT PRIMARY KEY,
 *     DATA VARBINARY(65535)
 * );
 * ```
 *
 * @param igniteClient Ignite 3.x 씬 클라이언트
 * @param config NearCache 설정 (tableName = Ignite 3.x 테이블 이름)
 */
class UserIgnite3R2dbcCacheRepository(
    igniteClient: IgniteClient,
    config: IgniteNearCacheConfig = IgniteNearCacheConfig(tableName = "TEST_USERS"),
): AbstractIgniteR2dbcCacheRepository<UserRecord, Long>(igniteClient, config) {

    companion object: KLoggingChannel()

    override val entityTable = UserSchema.UserTable

    override suspend fun ResultRow.toEntity(): UserRecord = toUserRecord()

    /**
     * Ignite 3.x [IgniteNearCache] 인스턴스를 생성합니다.
     * Caffeine(Front) + KeyValueView(Back) 2-Tier 구조입니다.
     */
    override fun createNearCache(): IgniteNearCache<Long, UserRecord> =
        IgniteNearCache(igniteClient, Long::class.java, UserRecord::class.java, config)
}

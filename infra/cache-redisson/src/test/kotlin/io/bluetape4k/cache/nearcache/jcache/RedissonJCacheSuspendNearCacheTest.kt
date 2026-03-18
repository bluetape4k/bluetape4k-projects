package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.time.Duration

/**
 * Redisson JCache 백엔드를 사용하는 [AbstractSuspendNearCacheTest] 구현체입니다.
 *
 * Caffeine front + Redisson SuspendCache back 2-tier SuspendNearCache 패턴을 검증합니다.
 */
class RedissonJCacheSuspendNearCacheTest: AbstractSuspendNearCacheTest() {

    companion object: KLoggingChannel()

    override val backSuspendCache: SuspendCache<String, Any> =
        RedissonSuspendCache(
            "redisson-jcache-suspend-back-" + Base58.randomString(12),
            RedisServers.redisson,
        )

    override fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendCache<String, Any> =
        CaffeineSuspendCache {
            expireAfterAccess(expireAfterAccess)
            maximumSize(100_000)
        }
}

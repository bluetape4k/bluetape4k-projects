package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.LettuceSuspendCacheManager
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import java.time.Duration

/**
 * Lettuce SuspendCache 백엔드를 사용하는 [AbstractSuspendNearJCacheTest] 구현체입니다.
 *
 * Caffeine front + Lettuce SuspendCache back 2-tier SuspendNearCache 패턴을 검증합니다.
 * [io.bluetape4k.cache.jcache.LettuceSuspendJCache]의 Channel 기반 [javax.cache.event.CacheEntryListener] 지원으로
 * 이벤트 전파가 동작합니다.
 */
class LettuceJCacheSuspendNearJCacheTest: AbstractSuspendNearJCacheTest() {

    companion object: KLoggingChannel()

    private val manager by lazy {
        LettuceSuspendCacheManager(RedisServers.redisClient, null, LettuceBinaryCodecs.lz4Fory())
    }

    override val backSuspendJCache: SuspendJCache<String, Any> =
        manager.getOrCreate("lettuce-jcache-suspend-back-" + Base58.randomString(12))

    override fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendJCache<String, Any> =
        CaffeineSuspendJCache {
            expireAfterAccess(expireAfterAccess)
            maximumSize(100_000)
        }
}

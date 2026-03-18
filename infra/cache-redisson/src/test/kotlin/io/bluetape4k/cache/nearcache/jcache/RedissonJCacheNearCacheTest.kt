package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.RedissonJCaching
import io.bluetape4k.logging.KLogging

/**
 * Redisson JCache 백엔드를 사용하는 [AbstractNearCacheTest] 구현체입니다.
 *
 * Caffeine front + Redisson JCache back 2-tier NearCache 패턴을 검증합니다.
 */
class RedissonJCacheNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> = RedissonJCaching.getOrCreate(
        cacheName = "redisson-jcache-back-" + randomKey(),
        redisson = RedisServers.redisson,
    )
}

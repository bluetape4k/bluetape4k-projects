package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.LettuceJCaching
import io.bluetape4k.logging.KLogging

/**
 * Lettuce JCache 백엔드를 사용하는 [AbstractNearCacheTest] 구현체입니다.
 *
 * Caffeine front + Lettuce JCache back 2-tier NearCache 패턴을 검증합니다.
 */
class LettuceJCacheNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> = LettuceJCaching.getOrCreate(
        redisClient = RedisServers.redisClient,
        cacheName = "lettuce-jcache-back-" + randomKey(),
    )
}

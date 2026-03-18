package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.RedissonCaches
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.logging.KLogging

/**
 * Redisson JCache 백엔드를 사용하는 [AbstractNearJCacheTest] 구현체입니다.
 *
 * Caffeine front + Redisson JCache back 2-tier NearCache 패턴을 검증합니다.
 */
class RedissonNearJCacheTest: AbstractNearJCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> =
        RedissonCaches.jcache(
            RedisServers.redisson,
            "redisson-jcache-back-" + randomKey(),
        )
}

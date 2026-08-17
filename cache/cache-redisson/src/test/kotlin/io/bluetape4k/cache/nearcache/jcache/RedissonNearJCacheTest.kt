package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.RedissonCaches
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

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

    @Test
    fun `기존 Redisson direct-back factory는 clear authority를 기본 거부한다`() {
        val nearCache = RedissonCaches.nearJCache(
            backCache,
            NearJCacheConfig<String, Any>(cacheName = "redisson-near-jcache-default-authority-" + randomKey()),
        )

        try {
            assertFailsWith<SecurityException> { nearCache.clear() }
        } finally {
            nearCache.close()
        }
    }

    @Test
    fun `Redisson direct-back factory는 explicit exclusive authority overload를 전달한다`() {
        val nearCache = RedissonCaches.nearJCache(
            backCache,
            NearJCacheConfig<String, Any>(cacheName = "redisson-near-jcache-exclusive-authority-" + randomKey()),
            EXCLUSIVE_BACK_CACHE,
        )

        try {
            nearCache.clearAllCache()
            nearCache.removeAll()
        } finally {
            nearCache.close()
        }
    }
}

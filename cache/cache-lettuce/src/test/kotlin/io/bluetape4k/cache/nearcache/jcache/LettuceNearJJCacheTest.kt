package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.LettuceCaches
import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

/**
 * Lettuce JCache 백엔드를 사용하는 [AbstractNearJCacheTest] 구현체입니다.
 *
 * Caffeine front + Lettuce JCache back 2-tier NearCache 패턴을 검증합니다.
 */
class LettuceNearJJCacheTest: AbstractNearJCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> =
        LettuceCaches.jcache(
            RedisServers.redisClient,
            "lettuce-near-jcache-back-" + randomKey()
        )

    @Test
    fun `기존 Lettuce factory는 clear authority를 기본 거부한다`() {
        val nearCache = LettuceCaches.nearJCache<String, String>(RedisServers.redisClient) {
            cacheName = "lettuce-near-jcache-default-authority-" + randomKey()
        }

        try {
            assertFailsWith<SecurityException> { nearCache.clear() }
        } finally {
            nearCache.close()
        }
    }

    @Test
    fun `Lettuce factory는 explicit exclusive authority overload를 전달한다`() {
        val nearCache = LettuceCaches.nearJCache<String, String>(
            RedisServers.redisClient,
            EXCLUSIVE_BACK_CACHE,
        ) {
            cacheName = "lettuce-near-jcache-exclusive-authority-" + randomKey()
            isSynchronous = true
        }

        try {
            nearCache.clearAllCache()
            nearCache.removeAll()
        } finally {
            nearCache.close()
        }
    }
}

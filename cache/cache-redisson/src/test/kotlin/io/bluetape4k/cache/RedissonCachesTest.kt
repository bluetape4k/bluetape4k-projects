package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.RedissonSuspendJCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.RedissonNearCache
import io.bluetape4k.cache.nearcache.RedissonSuspendNearCache
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.SuspendNearJCache
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.redisson.config.Config

/**
 * [RedissonCaches] 팩토리의 smoke test.
 * 각 팩토리 메서드가 올바른 타입의 인스턴스를 반환하는지 검증합니다.
 */
class RedissonCachesTest {
    companion object: KLogging() {
        private val redisson get() = RedisServers.redisson
    }

    @Test
    fun `jcache 팩토리는 JCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.jcache<String, String>(redisson, name)
        try {
            cache shouldBeInstanceOf JCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendJCache 팩토리는 RedissonSuspendJCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.suspendJCache<String, String>(redisson, name)
        try {
            cache shouldBeInstanceOf RedissonSuspendJCache::class
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `Config 기반 JCache 팩토리는 Redis 연결로 캐시를 생성한다`() {
        val name = RedisServers.randomName()
        val config = Config().apply {
            useSingleServer().setAddress(RedisServers.redisServer.url)
        }
        val cache = RedissonCaches.jcache<String, String>(name, config)
        try {
            cache shouldBeInstanceOf JCache::class
            cache.put("config-key", "config-value")
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `Config 기반 suspendJCache 팩토리는 Redis 연결로 캐시를 생성한다`() = runSuspendIO {
        val name = RedisServers.randomName()
        val config = Config().apply {
            useSingleServer().setAddress(RedisServers.redisServer.url)
        }
        val cache = RedissonCaches.suspendJCache<String, String>(name, config)
        try {
            cache shouldBeInstanceOf RedissonSuspendJCache::class
            cache.put("config-key", "config-value")
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `nearJCache 팩토리는 NearJCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.nearJCache<String, String>(name, redisson)
        try {
            cache shouldBeInstanceOf NearJCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearJCache 팩토리는 SuspendNearJCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.suspendNearJCache<String, String>(name, redisson)
        try {
            cache shouldBeInstanceOf SuspendNearJCache::class
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `nearCache 팩토리는 RedissonNearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.nearCache<String>(redisson)
        try {
            cache shouldBeInstanceOf NearCacheOperations::class
            cache shouldBeInstanceOf RedissonNearCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCache 팩토리는 RedissonSuspendNearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.suspendNearCache<String>(redisson)
        try {
            cache shouldBeInstanceOf SuspendNearCacheOperations::class
            cache shouldBeInstanceOf RedissonSuspendNearCache::class
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }
}

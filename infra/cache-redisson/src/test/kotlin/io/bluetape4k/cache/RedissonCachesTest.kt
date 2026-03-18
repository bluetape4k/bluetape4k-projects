package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.nearcache.jcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.RedissonNearCache
import io.bluetape4k.cache.nearcache.RedissonSuspendNearCache
import io.bluetape4k.cache.nearcache.jcache.SuspendNearCache
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

/**
 * [RedissonCaches] 팩토리의 smoke test.
 * 각 팩토리 메서드가 올바른 타입의 인스턴스를 반환하는지 검증합니다.
 */
class RedissonCachesTest {
    companion object : KLogging() {
        private val redisson get() = RedisServers.redisson
    }

    @Test
    fun `jcache 팩토리는 JCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.jcache<String, String>(name, redisson)
        try {
            cache shouldBeInstanceOf JCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendCache 팩토리는 RedissonSuspendCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.suspendCache<String, String>(name, redisson)
        try {
            cache shouldBeInstanceOf RedissonSuspendCache::class
        } finally {
            runCatching { runBlocking { cache.close() } }
        }
    }

    @Test
    fun `nearCache 팩토리는 NearCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.nearCache<String, String>(name, redisson)
        try {
            cache shouldBeInstanceOf NearCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCache 팩토리는 SuspendNearCache 인스턴스를 반환한다`() {
        val name = RedisServers.randomName()
        val cache = RedissonCaches.suspendNearCache<String, String>(name, redisson)
        try {
            cache shouldBeInstanceOf SuspendNearCache::class
        } finally {
            runCatching { runBlocking { cache.close() } }
        }
    }

    @Test
    fun `nearCacheOps 팩토리는 RedissonNearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.nearCacheOps<String>(redisson)
        try {
            cache shouldBeInstanceOf NearCacheOperations::class
            cache shouldBeInstanceOf RedissonNearCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCacheOps 팩토리는 RedissonSuspendNearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.suspendNearCacheOps<String>(redisson)
        try {
            cache shouldBeInstanceOf SuspendNearCacheOperations::class
            cache shouldBeInstanceOf RedissonSuspendNearCache::class
        } finally {
            runCatching { runBlocking { cache.close() } }
        }
    }
}

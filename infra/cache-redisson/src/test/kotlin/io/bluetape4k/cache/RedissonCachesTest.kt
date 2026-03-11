package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.RedissonResp3NearCache
import io.bluetape4k.cache.nearcache.RedissonResp3SuspendNearCache
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3NearCache
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3SuspendNearCache
import io.bluetape4k.cache.nearcache.SuspendNearCache
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
        private val redisClient get() = RedisServers.redisClient
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
    fun `resp3NearCache 팩토리는 RedissonResp3NearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.resp3NearCache<String>(redisson, redisClient)
        try {
            cache shouldBeInstanceOf RedissonResp3NearCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `resp3SuspendNearCache 팩토리는 RedissonResp3SuspendNearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.resp3SuspendNearCache<String>(redisson, redisClient)
        try {
            cache shouldBeInstanceOf RedissonResp3SuspendNearCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `resilientResp3NearCache 팩토리는 ResilientRedissonResp3NearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.resilientResp3NearCache<String>(redisson, redisClient)
        try {
            cache shouldBeInstanceOf ResilientRedissonResp3NearCache::class
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `resilientResp3SuspendNearCache 팩토리는 ResilientRedissonResp3SuspendNearCache 인스턴스를 반환한다`() {
        val cache = RedissonCaches.resilientResp3SuspendNearCache<String>(redisson, redisClient)
        try {
            cache shouldBeInstanceOf ResilientRedissonResp3SuspendNearCache::class
        } finally {
            runCatching { cache.close() }
        }
    }
}

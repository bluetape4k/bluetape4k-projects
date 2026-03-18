package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.ResilientNearCacheDecorator
import io.bluetape4k.cache.nearcache.ResilientSuspendNearCacheDecorator
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.withResilience
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58

class LettuceJCachesTest {

    companion object: KLogging() {
        private val redisClient by lazy { RedisServers.redisClient }
    }

    @Test
    fun `jcache - JCache 인스턴스를 반환한다`() {
        val name = "lettuce-caches-test-jcache-" + Base58.randomString(6)
        val cache = LettuceCaches.jcache<String, String>(redisClient, name)
        try {
            cache.shouldBeInstanceOf<JCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `nearCache - NearCacheOperations 인스턴스를 반환한다`() {
        val cache = LettuceCaches.nearCache<String>(redisClient)
        try {
            cache.shouldBeInstanceOf<LettuceNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCache - SuspendNearCacheOperations 인스턴스를 반환한다`() {
        val cache = LettuceCaches.suspendNearCache<String>(redisClient)
        try {
            cache.shouldBeInstanceOf<LettuceSuspendNearCache<*>>()
        } finally {
            runCatching { runBlocking { cache.close() } }
        }
    }

    @Test
    fun `nearCache withResilience - ResilientNearCacheDecorator 인스턴스를 반환한다`() {
        val cache: NearCacheOperations<String> = LettuceCaches.nearCache<String>(redisClient)
            .withResilience { retryMaxAttempts = 3 }
        try {
            cache.shouldBeInstanceOf<ResilientNearCacheDecorator<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCache withResilience - ResilientSuspendNearCacheDecorator 인스턴스를 반환한다`() {
        val cache: SuspendNearCacheOperations<String> = LettuceCaches.suspendNearCache<String>(redisClient)
            .withResilience { retryMaxAttempts = 3 }
        try {
            cache.shouldBeInstanceOf<ResilientSuspendNearCacheDecorator<*>>()
        } finally {
            runCatching { runBlocking { cache.close() } }
        }
    }
}

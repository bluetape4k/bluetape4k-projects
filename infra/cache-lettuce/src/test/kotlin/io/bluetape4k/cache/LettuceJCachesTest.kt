package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.LettuceSuspendJCache
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.ResilientNearCacheDecorator
import io.bluetape4k.cache.nearcache.ResilientSuspendNearCacheDecorator
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.SuspendNearJCache
import io.bluetape4k.cache.nearcache.withResilience
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
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

    // -------------------------------------------------------------------------
    // suspendJCache
    // -------------------------------------------------------------------------

    @Test
    fun `suspendJCache - LettuceSuspendJCache 인스턴스를 반환한다`() = runTest {
        val name = "lettuce-caches-test-suspend-jcache-" + Base58.randomString(6)
        val cache = LettuceCaches.suspendJCache<String>(redisClient, name)
        try {
            cache.shouldBeInstanceOf<LettuceSuspendJCache<*>>()
            cache.put("k1", "v1")
            cache.get("k1") shouldBeEqualTo "v1"
        } finally {
            runCatching { cache.close() }
        }
    }

    // -------------------------------------------------------------------------
    // nearJCache
    // -------------------------------------------------------------------------

    @Test
    fun `nearJCache DSL로 생성`() {
        val name = "lettuce-near-jcache-" + Base58.randomString(6)
        val cache = LettuceCaches.nearJCache<String, String>(redisClient) {
            cacheName = name
        }
        try {
            cache.shouldBeInstanceOf<NearJCache<*, *>>()
            cache.put("k1", "v1")
            cache.get("k1").shouldNotBeNull()
            cache.get("k1") shouldBeEqualTo "v1"
        } finally {
            runCatching { cache.close() }
        }
    }

    // -------------------------------------------------------------------------
    // suspendNearJCache
    // -------------------------------------------------------------------------

    @Test
    fun `suspendNearJCache DSL로 생성`() = runTest {
        val name = "lettuce-suspend-near-jcache-" + Base58.randomString(6)
        val cache = LettuceCaches.suspendNearJCache<String>(redisClient) {
            cacheName = name
        }
        try {
            cache.shouldBeInstanceOf<SuspendNearJCache<*, *>>()
            cache.put("k1", "v1")
            cache.get("k1") shouldBeEqualTo "v1"
        } finally {
            runCatching { cache.close() }
        }
    }
}

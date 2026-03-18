package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.HazelcastSuspendJCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.HazelcastNearCache
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfig
import io.bluetape4k.cache.nearcache.HazelcastSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.ResilientNearCacheDecorator
import io.bluetape4k.cache.nearcache.ResilientSuspendNearCacheDecorator
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.SuspendNearJCache
import io.bluetape4k.cache.nearcache.withResilience
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58

/**
 * [HazelcastCaches] 팩토리 smoke test.
 *
 * 각 팩토리 메서드가 올바른 타입의 인스턴스를 반환하는지 검증합니다.
 */
class HazelcastCachesTest {

    companion object: KLogging()

    private val hazelcastClient get() = HazelcastServers.hazelcastClient

    private fun randomName() = "test-cache-" + Base58.randomString(6)

    @Test
    fun `jcache - JCache 인스턴스 반환`() {
        val cache = HazelcastCaches.jcache<String, String>(hazelcastClient, randomName())
        try {
            cache.shouldBeInstanceOf<JCache<*, *>>()
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `suspendCache - HazelcastSuspendCache 인스턴스 반환`() = runTest {
        val cache = HazelcastCaches.suspendJCache<String, String>(hazelcastClient, randomName())
        try {
            cache.shouldBeInstanceOf<HazelcastSuspendJCache<*, *>>()
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `nearCache - HazelcastNearCache 인스턴스 반환`() {
        val config = HazelcastNearCacheConfig(cacheName = randomName())
        val cache = HazelcastCaches.nearCache<String>(hazelcastClient, config)
        try {
            cache.shouldBeInstanceOf<HazelcastNearCache<*>>()
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `suspendNearCache - HazelcastSuspendNearCache 인스턴스 반환`() {
        val config = HazelcastNearCacheConfig(cacheName = randomName())
        val cache = HazelcastCaches.suspendNearCache<String>(hazelcastClient, config)
        try {
            cache.shouldBeInstanceOf<HazelcastSuspendNearCache<*>>()
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `nearCache withResilience - ResilientNearCacheDecorator 인스턴스 반환`() {
        val config = HazelcastNearCacheConfig(cacheName = randomName())
        val cache: NearCacheOperations<String> = HazelcastCaches.nearCache<String>(hazelcastClient, config)
            .withResilience { retryMaxAttempts = 3 }
        try {
            cache.shouldBeInstanceOf<ResilientNearCacheDecorator<*>>()
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `suspendNearCache withResilience - ResilientSuspendNearCacheDecorator 인스턴스 반환`() = runTest {
        val config = HazelcastNearCacheConfig(cacheName = randomName())
        val cache: SuspendNearCacheOperations<String> = HazelcastCaches.suspendNearCache<String>(hazelcastClient, config)
            .withResilience { retryMaxAttempts = 3 }
        try {
            cache.shouldBeInstanceOf<ResilientSuspendNearCacheDecorator<*>>()
        } finally {
            runCatching { runSuspendIO { cache.close() } }
        }
    }

    @Test
    fun `nearJCache DSL로 생성 - NearJCache 인스턴스 반환`() {
        val cache = HazelcastCaches.nearJCache<String, String>(hazelcastClient) {
            cacheName = randomName()
        }
        try {
            cache.shouldBeInstanceOf<NearJCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearJCache DSL로 생성 - SuspendNearJCache 인스턴스 반환`() = runTest {
        val cache = HazelcastCaches.suspendNearJCache<String, String>(hazelcastClient) {
            cacheName = randomName()
        }
        try {
            cache.shouldBeInstanceOf<SuspendNearJCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }
}

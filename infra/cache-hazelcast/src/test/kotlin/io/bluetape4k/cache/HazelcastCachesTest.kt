package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.HazelcastSuspendCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.HazelcastNearCache
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfig
import io.bluetape4k.cache.nearcache.HazelcastSuspendNearCache
import io.bluetape4k.cache.nearcache.ResilientHazelcastNearCache
import io.bluetape4k.cache.nearcache.ResilientHazelcastSuspendNearCache
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
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendCache - HazelcastSuspendCache 인스턴스 반환`() = runTest {
        val cache = HazelcastCaches.suspendCache<String, String>(hazelcastClient, randomName())
        try {
            cache.shouldBeInstanceOf<HazelcastSuspendCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `nearCache - HazelcastNearCache 인스턴스 반환`() {
        val config = HazelcastNearCacheConfig(cacheName = randomName())
        val cache = HazelcastCaches.nearCache<String>(hazelcastClient, config)
        try {
            cache.shouldBeInstanceOf<HazelcastNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCache - HazelcastSuspendNearCache 인스턴스 반환`() {
        val config = HazelcastNearCacheConfig(cacheName = randomName())
        val cache = HazelcastCaches.suspendNearCache<String>(hazelcastClient, config)
        try {
            cache.shouldBeInstanceOf<HazelcastSuspendNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `resilientNearCache - ResilientHazelcastNearCache 인스턴스 반환`() {
        val nearCacheConfig = HazelcastNearCacheConfig(cacheName = randomName())
        val cache = HazelcastCaches.resilientNearCache<String>(hazelcastClient, nearCacheConfig)
        try {
            cache.shouldBeInstanceOf<ResilientHazelcastNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `resilientSuspendNearCache - ResilientHazelcastSuspendNearCache 인스턴스 반환`() {
        val nearCacheConfig = HazelcastNearCacheConfig(cacheName = randomName())
        val cache = HazelcastCaches.resilientSuspendNearCache<String>(hazelcastClient, nearCacheConfig)
        try {
            cache.shouldBeInstanceOf<ResilientHazelcastSuspendNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }
}

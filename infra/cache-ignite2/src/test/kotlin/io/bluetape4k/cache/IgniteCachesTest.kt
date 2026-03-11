package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.IgniteClientSuspendCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.SuspendNearCache
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import kotlin.time.Duration.Companion.seconds

class IgniteCachesTest {

    companion object: KLogging()

    @Test
    fun `jcache - JCache 인스턴스를 반환한다`() {
        val name = "ignite-caches-test-jcache-" + Base58.randomString(6)
        val cache = IgniteCaches.jcache<String, String>(name)
        try {
            cache.shouldBeInstanceOf<JCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `clientSuspendCache - IgniteClientSuspendCache 인스턴스를 반환한다`() = runSuspendIO {
        val clientCache = IgniteServers.getOrCreateCache<String, String>("ignite-caches-test-client-suspend")
        val cache = IgniteCaches.clientSuspendCache<String, String>(clientCache)
        try {
            cache.shouldBeInstanceOf<IgniteClientSuspendCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @RepeatedTest(3)
    fun `clientSuspendCache - 동적 cache 생성 직후에도 hang 없이 접근할 수 있다`() = runSuspendIO {
        val cacheName = "ignite-caches-test-dynamic-" + Base58.randomString(6)
        val key = "key-" + Base58.randomString(6)
        val value = "value-" + Base58.randomString(12)
        val clientCache = IgniteServers.getOrCreateCache<String, String>(cacheName)
        val cache = IgniteCaches.clientSuspendCache<String, String>(clientCache)

        try {
            val result = withTimeoutOrNull(10.seconds) {
                cache.put(key, value)
                cache.get(key)
            }

            result shouldBeEqualTo value
        } finally {
            runCatching { clientCache.clear() }
            runCatching { cache.close() }
        }
    }

    @Test
    fun `nearCache - NearCache 인스턴스를 반환한다`() {
        val name = "ignite-caches-test-near-" + Base58.randomString(6)
        val backCache = IgniteCaches.jcache<String, String>(name)
        val cache = IgniteCaches.nearCache<String, String>(backCache)
        try {
            cache.shouldBeInstanceOf<NearCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCache - SuspendNearCache 인스턴스를 반환한다`() = runSuspendIO {
        val backCacheName = "ignite-caches-test-suspend-near-" + Base58.randomString(6)
        val frontCache = CaffeineSuspendCache<String, String>()
        val backCache = IgniteCaches.suspendCache<String, String>(backCacheName)
        val cache = IgniteCaches.suspendNearCache<String, String>(frontCache, backCache)
        try {
            cache.shouldBeInstanceOf<SuspendNearCache<*, *>>()
        } finally {
            runCatching { cache.close() }
        }
    }
}

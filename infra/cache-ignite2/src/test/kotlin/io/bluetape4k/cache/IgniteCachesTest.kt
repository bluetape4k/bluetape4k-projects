package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.IgniteClientSuspendCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.SuspendNearCache
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58

class IgniteCachesTest {

    companion object : KLogging() {
        private val igniteClient by lazy { IgniteServers.igniteClient }
    }

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
    fun `clientSuspendCache - IgniteClientSuspendCache 인스턴스를 반환한다`() {
        val name = "ignite-caches-test-client-suspend-" + Base58.randomString(6)
        val clientCache = igniteClient.getOrCreateCache<String, String>(name)
        val cache = IgniteCaches.clientSuspendCache<String, String>(clientCache)
        try {
            cache.shouldBeInstanceOf<IgniteClientSuspendCache<*, *>>()
        } finally {
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
    fun `suspendNearCache - SuspendNearCache 인스턴스를 반환한다`() {
        val backCacheName = "ignite-caches-test-suspend-near-" + Base58.randomString(6)
        val frontCache = CaffeineSuspendCache<String, String>()
        val backCache = IgniteCaches.suspendCache<String, String>(backCacheName)
        val cache = IgniteCaches.suspendNearCache<String, String>(frontCache, backCache)
        try {
            cache.shouldBeInstanceOf<SuspendNearCache<*, *>>()
        } finally {
            runCatching { runBlocking { cache.close() } }
        }
    }
}

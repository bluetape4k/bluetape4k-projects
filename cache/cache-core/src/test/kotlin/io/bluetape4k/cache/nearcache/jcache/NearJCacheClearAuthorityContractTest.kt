package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheManagementMXBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import javax.cache.Cache

class NearJCacheClearAuthorityContractTest {

    @Test
    fun `기존 direct constructor는 namespace-wide clear를 기본 거부하고 mutation 전에 실패한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val cacheName = "near-jcache-authority-contract"
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(cacheName = cacheName),
        )

        listOf(
            "clear" to { nearCache.clear() },
            "clearAllCache" to { nearCache.clearAllCache() },
            "removeAll" to { nearCache.removeAll() },
        ).forEach { (operation, invoke) ->
            val error = assertFailsWith<SecurityException> { invoke() }

            error.message.orEmpty().contains(operation).shouldBeEqualTo(true)
            error.message.orEmpty().contains("DENY").shouldBeEqualTo(true)
            error.message.orEmpty().contains(cacheName).shouldBeEqualTo(false)
            verify(exactly = 0) { frontCache.clear() }
            verify(exactly = 0) { frontCache.removeAll() }
            verify(exactly = 0) { backCache.clear() }
            verify(exactly = 0) { backCache.remove(any()) }
        }
    }

    @Test
    fun `기존 config factory도 기본 DENY를 유지한다`() {
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val nearCache = NearJCache(
            nearCacheCfg = NearJCacheConfig(cacheName = "near-jcache-default-factory"),
            backCache = backCache,
        )

        assertFailsWith<SecurityException> { nearCache.clear() }

        verify(exactly = 0) { backCache.clear() }
    }

    @Test
    fun `explicit exclusive owner는 세 namespace-wide operation을 수행한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.iterator() } returns mutableListOf<Cache.Entry<String, String>>().iterator()
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
            clearAuthority = NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
        )

        nearCache.clear()
        nearCache.clearAllCache()
        nearCache.removeAll()

        verify(exactly = 2) { frontCache.clear() }
        verify(exactly = 1) { frontCache.removeAll() }
        verify(exactly = 2) { backCache.clear() }
    }

    @Test
    fun `key-scoped removeAll은 DENY에서도 기존 범위로 동작한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        nearCache.removeAll(setOf("key"))

        verify(exactly = 1) { frontCache.removeAll(setOf("key")) }
        verify(exactly = 1) { backCache.remove("key") }
    }

    @Test
    fun `management snapshot은 authority를 stable token으로 노출한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val denied = NearJCache(frontCache, backCache, NearJCacheConfig())
        val exclusive = NearJCache(
            frontCache,
            backCache,
            NearJCacheConfig(),
            NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
        )

        NearJCacheManagementMXBean(denied).getClearAuthority() shouldBeEqualTo "DENY"
        NearJCacheManagementMXBean(exclusive).getClearAuthority() shouldBeEqualTo "EXCLUSIVE_BACK_CACHE"
    }
}

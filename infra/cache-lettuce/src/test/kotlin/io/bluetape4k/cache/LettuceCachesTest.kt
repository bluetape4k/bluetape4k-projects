package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.cache.nearcache.ResilientLettuceNearCache
import io.bluetape4k.cache.nearcache.ResilientLettuceSuspendNearCache
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58

class LettuceCachesTest {

    companion object : KLogging() {
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
    fun `nearCache - LettuceNearCache 인스턴스를 반환한다`() {
        val cache = LettuceCaches.nearCache<String>(redisClient)
        try {
            cache.shouldBeInstanceOf<LettuceNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `suspendNearCache - LettuceSuspendNearCache 인스턴스를 반환한다`() {
        val cache = LettuceCaches.suspendNearCache<String>(redisClient)
        try {
            cache.shouldBeInstanceOf<LettuceSuspendNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `resilientNearCache - ResilientLettuceNearCache 인스턴스를 반환한다`() {
        val cache = LettuceCaches.resilientNearCache<String>(redisClient)
        try {
            cache.shouldBeInstanceOf<ResilientLettuceNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `resilientSuspendNearCache - ResilientLettuceSuspendNearCache 인스턴스를 반환한다`() {
        val cache = LettuceCaches.resilientSuspendNearCache<String>(redisClient)
        try {
            cache.shouldBeInstanceOf<ResilientLettuceSuspendNearCache<*>>()
        } finally {
            runCatching { cache.close() }
        }
    }
}

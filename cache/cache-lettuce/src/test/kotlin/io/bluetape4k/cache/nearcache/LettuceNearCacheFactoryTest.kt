package io.bluetape4k.cache.nearcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.time.Duration

class LettuceNearCacheFactoryTest {

    @Test
    fun `config DSL preserves all cache settings and composes Redis keys`() {
        val config = lettuceNearCacheConfig<String, String> {
            cacheName = "factory-${Base58.randomString(6)}"
            maxLocalSize = 17
            frontExpireAfterWrite = Duration.ofSeconds(11)
            frontExpireAfterAccess = Duration.ofSeconds(7)
            redisTtl = Duration.ofSeconds(5)
            useRespProtocol3 = false
            recordStats = true
        }

        config.maxLocalSize shouldBeEqualTo 17L
        config.frontExpireAfterWrite shouldBeEqualTo Duration.ofSeconds(11)
        config.frontExpireAfterAccess shouldBeEqualTo Duration.ofSeconds(7)
        config.redisTtl shouldBeEqualTo Duration.ofSeconds(5)
        config.useRespProtocol3.shouldBeFalse()
        config.recordStats.shouldBeTrue()
        config.redisKey("user:1") shouldBeEqualTo "${config.cacheName}:user:1"
    }

    @Test
    fun `config builder rejects invalid boundaries`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfigBuilder<String, String>().apply { cacheName = "" }.build()
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfigBuilder<String, String>().apply { cacheName = "bad:name" }.build()
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfigBuilder<String, String>().apply { maxLocalSize = 0 }.build()
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfigBuilder<String, String>().apply {
                frontExpireAfterWrite = Duration.ZERO
            }.build()
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfigBuilder<String, String>().apply {
                frontExpireAfterAccess = Duration.ZERO
            }.build()
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfigBuilder<String, String>().apply {
                redisTtl = Duration.ZERO
            }.build()
        }
    }

    @Test
    fun `blocking top-level factory creates a configured cache`() {
        val cacheName = "factory-blocking-${Base58.randomString(6)}"
        val cache: NearCacheOperations<String> = lettuceNearCacheOf(
            RedisServers.redisClient,
            LettuceBinaryCodecs.default(),
            LettuceNearCacheConfig(cacheName = cacheName)
        )

        try {
            cache.cacheName shouldBeEqualTo cacheName
            cache.isClosed.shouldBeFalse()
            cache.put("user:1", "Alice")
            cache.get("user:1") shouldBeEqualTo "Alice"
        } finally {
            cache.close()
        }

        cache.isClosed.shouldBeTrue()
    }

    @Test
    fun `suspend top-level factory creates a configured cache`() = runTest {
        val cacheName = "factory-suspend-${Base58.randomString(6)}"
        val cache: SuspendNearCacheOperations<String> = lettuceSuspendNearCacheOf(
            RedisServers.redisClient,
            LettuceBinaryCodecs.default(),
            LettuceNearCacheConfig(cacheName = cacheName)
        )

        try {
            cache.cacheName shouldBeEqualTo cacheName
            cache.isClosed.shouldBeFalse()
            cache.put("order:1", "created")
            cache.get("order:1") shouldBeEqualTo "created"
        } finally {
            runSuspendIO { cache.close() }
        }

        cache.isClosed.shouldBeTrue()
    }
}

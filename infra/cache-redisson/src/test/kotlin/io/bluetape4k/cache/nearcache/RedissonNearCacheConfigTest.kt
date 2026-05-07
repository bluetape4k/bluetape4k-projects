package io.bluetape4k.cache.nearcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.redisson.api.options.LocalCachedMapOptions
import java.time.Duration
import kotlin.test.assertFailsWith

class RedissonNearCacheConfigTest {

    @Test
    fun `유효한 near cache 설정은 그대로 생성된다`() {
        val config = RedissonNearCacheConfig(
            cacheName = "orders-near",
            maxLocalSize = 500,
            timeToLive = Duration.ofSeconds(30),
            maxIdle = Duration.ofSeconds(10),
            syncStrategy = LocalCachedMapOptions.SyncStrategy.UPDATE,
        )

        config.cacheName shouldBeEqualTo "orders-near"
        config.maxLocalSize shouldBeEqualTo 500
        config.timeToLive shouldBeEqualTo Duration.ofSeconds(30)
        config.maxIdle shouldBeEqualTo Duration.ofSeconds(10)
    }

    @Test
    fun `cacheName 과 maxLocalSize 는 유효해야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            RedissonNearCacheConfig(cacheName = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            RedissonNearCacheConfig(maxLocalSize = 0)
        }
    }

    @Test
    fun `timeToLive 와 maxIdle 은 지정 시 0보다 커야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            RedissonNearCacheConfig(timeToLive = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            RedissonNearCacheConfig(timeToLive = Duration.ofSeconds(-1))
        }
        assertFailsWith<IllegalArgumentException> {
            RedissonNearCacheConfig(maxIdle = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            RedissonNearCacheConfig(maxIdle = Duration.ofSeconds(-1))
        }
    }

    @Test
    fun `redissonNearCacheConfig DSL 빌더로 설정을 생성할 수 있다`() {
        val config = redissonNearCacheConfig {
            cacheName = "dsl-cache"
            maxLocalSize = 2_000
            timeToLive = Duration.ofMinutes(10)
            maxIdle = Duration.ofMinutes(5)
            syncStrategy = LocalCachedMapOptions.SyncStrategy.UPDATE
            reconnectionStrategy = LocalCachedMapOptions.ReconnectionStrategy.LOAD
            evictionPolicy = LocalCachedMapOptions.EvictionPolicy.LFU
        }

        config shouldBeInstanceOf RedissonNearCacheConfig::class
        config.cacheName shouldBeEqualTo "dsl-cache"
        config.maxLocalSize shouldBeEqualTo 2_000
        config.timeToLive shouldBeEqualTo Duration.ofMinutes(10)
        config.maxIdle shouldBeEqualTo Duration.ofMinutes(5)
        config.syncStrategy shouldBeEqualTo LocalCachedMapOptions.SyncStrategy.UPDATE
        config.reconnectionStrategy shouldBeEqualTo LocalCachedMapOptions.ReconnectionStrategy.LOAD
        config.evictionPolicy shouldBeEqualTo LocalCachedMapOptions.EvictionPolicy.LFU
    }

    @Test
    fun `redissonNearCacheConfig DSL 빌더는 기본값을 유지한다`() {
        val config = redissonNearCacheConfig { }

        config.cacheName shouldBeEqualTo "redisson-near-cache"
        config.maxLocalSize shouldBeEqualTo 10_000
        config.syncStrategy shouldBeEqualTo LocalCachedMapOptions.SyncStrategy.INVALIDATE
        config.reconnectionStrategy shouldBeEqualTo LocalCachedMapOptions.ReconnectionStrategy.CLEAR
        config.evictionPolicy shouldBeEqualTo LocalCachedMapOptions.EvictionPolicy.LRU
    }

    @Test
    fun `redissonNearCacheConfig DSL 빌더 - 잘못된 값은 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            redissonNearCacheConfig { cacheName = "" }
        }
        assertFailsWith<IllegalArgumentException> {
            redissonNearCacheConfig { maxLocalSize = -1 }
        }
    }
}

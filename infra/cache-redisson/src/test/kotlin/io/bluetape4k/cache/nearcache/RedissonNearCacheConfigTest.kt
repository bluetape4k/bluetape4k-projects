package io.bluetape4k.cache.nearcache

import org.amshove.kluent.shouldBeEqualTo
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
}

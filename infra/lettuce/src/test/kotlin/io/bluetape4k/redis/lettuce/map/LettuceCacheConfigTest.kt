package io.bluetape4k.redis.lettuce.map

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class LettuceCacheConfigTest {

    @Test
    fun `유효한 설정은 그대로 생성된다`() {
        val config = LettuceCacheConfig(
            writeBehindBatchSize = 10,
            writeBehindQueueCapacity = 100,
            writeRetryAttempts = 2,
            ttl = Duration.ofMinutes(5),
            keyPrefix = "cache-prefix",
            nearCacheName = "near-cache",
            nearCacheMaxSize = 1_000,
            nearCacheTtl = Duration.ofSeconds(30),
        )

        config.writeBehindBatchSize shouldBeEqualTo 10
        config.writeRetryAttempts shouldBeEqualTo 2
        config.keyPrefix shouldBeEqualTo "cache-prefix"
    }

    @Test
    fun `batch size queue size retry attempts 는 0보다 커야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(writeBehindBatchSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(writeBehindQueueCapacity = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(writeRetryAttempts = 0)
        }
    }

    @Test
    fun `ttl keyPrefix near cache 설정은 유효해야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(ttl = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(keyPrefix = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(nearCacheName = "")
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(nearCacheMaxSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceCacheConfig(nearCacheTtl = Duration.ZERO)
        }
    }
}

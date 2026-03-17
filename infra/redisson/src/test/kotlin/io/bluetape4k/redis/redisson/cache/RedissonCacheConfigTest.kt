package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.redisson.api.map.WriteMode
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.LocalCachedMapParams
import org.redisson.api.options.MapParams
import java.time.Duration

class RedissonCacheConfigTest {

    @Test
    fun `toMapOptions applies codec and write-behind settings`() {
        val config = RedissonCacheConfig(
            writeMode = WriteMode.WRITE_BEHIND,
            codec = RedissonCodecs.String,
            writeBehindBatchSize = 128,
            writeBehindDelay = 250,
            writeRetryAttempts = 5,
            writeRetryInterval = Duration.ofMillis(750),
        )

        val options = config.toMapOptions<String, String>("users")
        val params = options as MapParams<String, String>

        params.name shouldBeEqualTo "users"
        assertSame(RedissonCodecs.String, params.codec)
        params.writeMode shouldBeEqualTo WriteMode.WRITE_BEHIND
        params.writeBehindBatchSize shouldBeEqualTo 128
        params.writeBehindDelay shouldBeEqualTo 250
        params.writeRetryAttempts shouldBeEqualTo 5
        params.writeRetryInterval shouldBeEqualTo 750L
    }

    @Test
    fun `toLocalCachedMapOptions applies codec and near-cache settings`() {
        val config = RedissonCacheConfig(
            cacheMode = RedissonCacheConfig.CacheMode.READ_ONLY,
            codec = RedissonCodecs.String,
            nearCacheEnabled = true,
            nearCacheMaxSize = 256,
            nearCacheTtl = Duration.ofSeconds(30),
            nearCacheMaxIdleTime = Duration.ofSeconds(10),
            nearCacheSyncStrategy = LocalCachedMapOptions.SyncStrategy.INVALIDATE,
        )

        val options = config.toLocalCachedMapOptions<String, String>("profiles")
        val params = options as LocalCachedMapParams<String, String>

        params.name shouldBeEqualTo "profiles"
        assertSame(RedissonCodecs.String, params.codec)
        params.cacheSize shouldBeEqualTo 256
        params.timeToLiveInMillis shouldBeEqualTo 30_000L
        params.maxIdleInMillis shouldBeEqualTo 10_000L
        params.syncStrategy shouldBeEqualTo LocalCachedMapOptions.SyncStrategy.INVALIDATE
        params.evictionPolicy shouldBeEqualTo LocalCachedMapOptions.EvictionPolicy.LRU
    }

    @Test
    fun `unsupported settings fail fast during option conversion`() {
        assertThrows<IllegalArgumentException> {
            RedissonCacheConfig(ttl = Duration.ofSeconds(1))
                .toMapOptions<String, String>("ttl-cache")
        }

        assertThrows<IllegalArgumentException> {
            RedissonCacheConfig(maxSize = 100)
                .toLocalCachedMapOptions<String, String>("bounded-cache")
        }

        assertThrows<IllegalArgumentException> {
            RedissonCacheConfig(deleteFromDBOnInvalidate = true)
                .toMapOptions<String, String>("invalidate-cache")
        }
    }
}

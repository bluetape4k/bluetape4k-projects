package io.bluetape4k.cache.nearcache

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class HazelcastNearCacheConfigTest {

    @Test
    fun `유효한 near cache 설정은 그대로 생성된다`() {
        val config = HazelcastNearCacheConfig(
            cacheName = "hz-near",
            maxLocalSize = 1_000,
            frontExpireAfterWrite = Duration.ofSeconds(30),
            frontExpireAfterAccess = Duration.ofSeconds(10),
            recordStats = true,
        )

        config.cacheName shouldBeEqualTo "hz-near"
        config.maxLocalSize shouldBeEqualTo 1_000
        config.frontExpireAfterWrite shouldBeEqualTo Duration.ofSeconds(30)
        config.frontExpireAfterAccess shouldBeEqualTo Duration.ofSeconds(10)
    }

    @Test
    fun `cacheName maxLocalSize duration 은 유효해야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(cacheName = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(maxLocalSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(frontExpireAfterWrite = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(frontExpireAfterAccess = Duration.ZERO)
        }
    }
}

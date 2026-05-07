package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cache.nearcache.GetFailureStrategy
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class ResilientNearJCacheConfigBuilderTest {

    companion object: KLogging()

    @Test
    fun `기본값으로 ResilientNearJCacheConfig DSL 빌더 생성`() {
        val config = resilientNearJCacheConfig<String, Int> { }

        config.cacheName shouldBeEqualTo "resilient-near-cache"
        config.maxLocalSize shouldBeEqualTo 10_000L
        config.frontExpireAfterWrite shouldBeEqualTo Duration.ofMinutes(30)
        config.frontExpireAfterAccess.shouldBeNull()
        config.recordStats.shouldBeFalse()
        config.writeQueueCapacity shouldBeEqualTo 1024
        config.retryMaxAttempts shouldBeEqualTo 3
        config.retryWaitDuration shouldBeEqualTo Duration.ofMillis(500)
        config.retryExponentialBackoff.shouldBeTrue()
        config.getFailureStrategy shouldBeEqualTo GetFailureStrategy.RETURN_FRONT_OR_NULL
    }

    @Test
    fun `커스텀 값으로 ResilientNearJCacheConfig DSL 빌더 생성`() {
        val config = resilientNearJCacheConfig<String, String> {
            cacheName = "my-resilient-cache"
            maxLocalSize = 5000L
            frontExpireAfterWrite = Duration.ofMinutes(10)
            frontExpireAfterAccess = Duration.ofMinutes(5)
            recordStats = true
            writeQueueCapacity = 512
            retryMaxAttempts = 5
            retryWaitDuration = Duration.ofSeconds(1)
            retryExponentialBackoff = false
            getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
        }

        config.cacheName shouldBeEqualTo "my-resilient-cache"
        config.maxLocalSize shouldBeEqualTo 5000L
        config.frontExpireAfterWrite shouldBeEqualTo Duration.ofMinutes(10)
        config.frontExpireAfterAccess.shouldNotBeNull()
        config.frontExpireAfterAccess shouldBeEqualTo Duration.ofMinutes(5)
        config.recordStats.shouldBeTrue()
        config.writeQueueCapacity shouldBeEqualTo 512
        config.retryMaxAttempts shouldBeEqualTo 5
        config.retryWaitDuration shouldBeEqualTo Duration.ofSeconds(1)
        config.retryExponentialBackoff.shouldBeFalse()
        config.getFailureStrategy shouldBeEqualTo GetFailureStrategy.PROPAGATE_EXCEPTION
    }

    @Test
    fun `builder build - 기본값으로 생성`() {
        val builder = ResilientNearJCacheConfigBuilder<String, Int>()
        val config = builder.build()

        config.cacheName shouldBeEqualTo "resilient-near-cache"
        config.maxLocalSize shouldBeEqualTo 10_000L
        config.frontExpireAfterAccess.shouldBeNull()
        config.recordStats.shouldBeFalse()
    }

    @Test
    fun `builder build - frontExpireAfterAccess 설정 가능`() {
        val builder = ResilientNearJCacheConfigBuilder<String, String>().apply {
            frontExpireAfterAccess = Duration.ofMinutes(15)
        }
        val config = builder.build()

        config.frontExpireAfterAccess.shouldNotBeNull()
        config.frontExpireAfterAccess shouldBeEqualTo Duration.ofMinutes(15)
    }

    @Test
    fun `builder build - frontExpireAfterAccess null 유지`() {
        val config = resilientNearJCacheConfig<String, Int> {
            frontExpireAfterAccess = null
        }

        config.frontExpireAfterAccess.shouldBeNull()
    }

    @Test
    fun `retryMaxAttempts 가 0 이하면 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            resilientNearJCacheConfig<String, Int> {
                retryMaxAttempts = 0
            }
        }
    }

    @Test
    fun `maxLocalSize 가 0 이하면 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            resilientNearJCacheConfig<String, Int> {
                maxLocalSize = 0L
            }
        }
    }

    @Test
    fun `writeQueueCapacity 가 0 이하면 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            resilientNearJCacheConfig<String, Int> {
                writeQueueCapacity = 0
            }
        }
    }
}

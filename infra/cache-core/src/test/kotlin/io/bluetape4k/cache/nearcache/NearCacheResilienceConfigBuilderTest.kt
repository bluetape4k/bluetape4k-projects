package io.bluetape4k.cache.nearcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class NearCacheResilienceConfigBuilderTest {

    companion object: KLogging()

    @Test
    fun `기본값으로 NearCacheResilienceConfig DSL 빌더 생성`() {
        val config = nearCacheResilienceConfig { }

        config.retryMaxAttempts shouldBeEqualTo 3
        config.retryWaitDuration shouldBeEqualTo Duration.ofMillis(500)
        config.retryExponentialBackoff shouldBeEqualTo true
        config.getFailureStrategy shouldBeEqualTo GetFailureStrategy.RETURN_FRONT_OR_NULL
    }

    @Test
    fun `커스텀 값으로 NearCacheResilienceConfig DSL 빌더 생성`() {
        val config = nearCacheResilienceConfig {
            retryMaxAttempts = 5
            retryWaitDuration = Duration.ofSeconds(1)
            retryExponentialBackoff = false
            getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
        }

        config.retryMaxAttempts shouldBeEqualTo 5
        config.retryWaitDuration shouldBeEqualTo Duration.ofSeconds(1)
        config.retryExponentialBackoff.shouldBeFalse()
        config.getFailureStrategy shouldBeEqualTo GetFailureStrategy.PROPAGATE_EXCEPTION
    }

    @Test
    fun `builder build - 기본값으로 생성`() {
        val builder = NearCacheResilienceConfigBuilder()
        val config = builder.build()

        config.retryMaxAttempts shouldBeEqualTo 3
        config.retryWaitDuration shouldBeEqualTo Duration.ofMillis(500)
        config.retryExponentialBackoff.shouldBeTrue()
        config.getFailureStrategy shouldBeEqualTo GetFailureStrategy.RETURN_FRONT_OR_NULL
    }

    @Test
    fun `builder build - 커스텀 값으로 생성`() {
        val builder = NearCacheResilienceConfigBuilder().apply {
            retryMaxAttempts = 10
            retryWaitDuration = Duration.ofSeconds(2)
            retryExponentialBackoff = false
            getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
        }
        val config = builder.build()

        config.retryMaxAttempts shouldBeEqualTo 10
        config.retryWaitDuration shouldBeEqualTo Duration.ofSeconds(2)
        config.retryExponentialBackoff.shouldBeFalse()
        config.getFailureStrategy shouldBeEqualTo GetFailureStrategy.PROPAGATE_EXCEPTION
    }

    @Test
    fun `retryMaxAttempts 가 0 이하면 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            nearCacheResilienceConfig {
                retryMaxAttempts = 0
            }
        }
    }

    @Test
    fun `retryMaxAttempts 가 음수이면 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            nearCacheResilienceConfig {
                retryMaxAttempts = -1
            }
        }
    }
}

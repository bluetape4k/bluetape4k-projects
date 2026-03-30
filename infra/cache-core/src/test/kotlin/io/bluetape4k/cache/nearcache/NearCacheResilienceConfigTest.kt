package io.bluetape4k.cache.nearcache

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class NearCacheResilienceConfigTest {

    @Test
    fun `유효한 resilience 설정은 그대로 생성된다`() {
        val config = NearCacheResilienceConfig(
            retryMaxAttempts = 5,
            retryWaitDuration = Duration.ofSeconds(1),
            retryExponentialBackoff = false,
            getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION,
        )

        config.retryMaxAttempts shouldBeEqualTo 5
        config.retryWaitDuration shouldBeEqualTo Duration.ofSeconds(1)
        config.retryExponentialBackoff shouldBeEqualTo false
    }

    @Test
    fun `retryMaxAttempts 와 retryWaitDuration 은 유효해야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            NearCacheResilienceConfig(retryMaxAttempts = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            NearCacheResilienceConfig(retryWaitDuration = Duration.ZERO)
        }
    }
}

package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.local.LocalSuspendBucketProvider
import io.bluetape4k.bucket4j.ratelimit.AbstractSuspendRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.RateLimitStatus
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class LocalSuspendRateLimiterTest: AbstractSuspendRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider = LocalSuspendBucketProvider(defaultBucketConfiguration)

    override val rateLimiter: SuspendRateLimiter<String> = LocalSuspendRateLimiter(bucketProvider)

    @Test
    fun `토큰 부족 시 즉시 거절해야 한다`() = runSuspendIO {
        val key = randomKey()

        rateLimiter.consume(key, INITIAL_CAPACITY)

        withTimeout(1.seconds) {
            val result = rateLimiter.consume(key, 1)
            result.status shouldBeEqualTo RateLimitStatus.REJECTED
            result.consumedTokens shouldBeEqualTo 0L
        }
    }

    @Test
    fun `취소 예외는 전파해야 한다`() = runSuspendIO {
        val brokenProvider = mockk<LocalSuspendBucketProvider>()
        every { brokenProvider.resolveBucket(any()) } throws CancellationException("simulated cancellation")

        val limiter = LocalSuspendRateLimiter(brokenProvider)

        assertFailsWith<CancellationException> {
            limiter.consume(randomKey(), 1)
        }
    }
}

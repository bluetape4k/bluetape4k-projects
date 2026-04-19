package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.local.LocalBucketProvider
import io.bluetape4k.bucket4j.ratelimit.AbstractRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.RateLimitStatus
import io.bluetape4k.bucket4j.ratelimit.RateLimiter
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class LocalRateLimiterTest: AbstractRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider = LocalBucketProvider(defaultBucketConfiguration)

    override val rateLimiter: RateLimiter<String> = LocalRateLimiter(bucketProvider)

    @Test
    fun `CancellationException 은 ERROR 로 변환되지 않고 그대로 전파되어야 한다`() {
        // CancellationException 은 Exception 하위 타입이므로 명시적으로 재전파하지 않으면 ERROR 결과로 변환될 수 있다.
        val brokenProvider = mockk<LocalBucketProvider>()
        every { brokenProvider.resolveBucket(any()) } throws CancellationException("simulated cancellation")

        val limiter = LocalRateLimiter(brokenProvider)

        assertFailsWith<CancellationException> {
            limiter.consume(randomKey(), 1)
        }
    }

    @Test
    fun `버킷 조회 중 일반 예외 발생 시 error 결과를 반환한다`() {
        val brokenProvider = mockk<LocalBucketProvider>()
        every { brokenProvider.resolveBucket(any()) } throws RuntimeException("simulated bucket failure")

        val limiter = LocalRateLimiter(brokenProvider)
        val result = limiter.consume(randomKey(), 1)
        result.status shouldBeEqualTo RateLimitStatus.ERROR
    }
}

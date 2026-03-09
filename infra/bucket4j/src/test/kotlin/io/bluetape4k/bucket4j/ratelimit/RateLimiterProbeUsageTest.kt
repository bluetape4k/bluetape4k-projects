package io.bluetape4k.bucket4j.ratelimit

import io.bluetape4k.bucket4j.coroutines.SuspendLocalBucket
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.bucket4j.distributed.BucketProxyProvider
import io.bluetape4k.bucket4j.local.LocalBucketProvider
import io.bluetape4k.bucket4j.local.LocalSuspendBucketProvider
import io.bluetape4k.bucket4j.ratelimit.distributed.DistributedRateLimiter
import io.bluetape4k.bucket4j.ratelimit.distributed.DistributedSuspendRateLimiter
import io.bluetape4k.bucket4j.ratelimit.local.LocalRateLimiter
import io.bluetape4k.bucket4j.ratelimit.local.LocalSuspendRateLimiter
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.BucketProxy
import io.github.bucket4j.local.LocalBucket
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class RateLimiterProbeUsageTest {

    @Test
    fun `local rate limiter 는 probe 기반으로 남은 토큰을 계산한다`() {
        val bucketProvider = mockk<LocalBucketProvider>()
        val bucket = mockk<LocalBucket>()
        every { bucketProvider.resolveBucket("user:1") } returns bucket
        every { bucket.tryConsumeAndReturnRemaining(3) } returns ConsumptionProbe.consumed(7, 0)

        val result = LocalRateLimiter(bucketProvider).consume("user:1", 3)

        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.consumedTokens shouldBeEqualTo 3
        result.availableTokens shouldBeEqualTo 7

        verify(exactly = 1) { bucket.tryConsumeAndReturnRemaining(3) }
        verify(exactly = 0) { bucket.availableTokens }
    }

    @Test
    fun `distributed rate limiter 는 probe 기반으로 남은 토큰을 계산한다`() {
        val bucketProvider = mockk<BucketProxyProvider>()
        val bucket = mockk<BucketProxy>()
        every { bucketProvider.resolveBucket("user:1") } returns bucket
        every { bucket.tryConsumeAndReturnRemaining(2) } returns ConsumptionProbe.rejected(8, 11, 11)

        val result = DistributedRateLimiter(bucketProvider).consume("user:1", 2)

        result.status shouldBeEqualTo RateLimitStatus.REJECTED
        result.consumedTokens shouldBeEqualTo 0
        result.availableTokens shouldBeEqualTo 8

        verify(exactly = 1) { bucket.tryConsumeAndReturnRemaining(2) }
        verify(exactly = 0) { bucket.availableTokens }
    }

    @Test
    fun `local suspend rate limiter 는 probe 기반으로 남은 토큰을 계산한다`() = runTest {
        val bucketProvider = mockk<LocalSuspendBucketProvider>()
        val bucket = mockk<SuspendLocalBucket>()
        every { bucketProvider.resolveBucket("user:1") } returns bucket
        every { bucket.tryConsumeAndReturnRemaining(4) } returns ConsumptionProbe.consumed(6, 0)

        val result = LocalSuspendRateLimiter(bucketProvider).consume("user:1", 4)

        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.consumedTokens shouldBeEqualTo 4
        result.availableTokens shouldBeEqualTo 6

        verify(exactly = 1) { bucket.tryConsumeAndReturnRemaining(4) }
        verify(exactly = 0) { bucket.availableTokens }
    }

    @Test
    fun `distributed suspend rate limiter 는 probe future 하나만 await 한다`() = runTest {
        val bucketProvider = mockk<AsyncBucketProxyProvider>()
        val bucket = mockk<AsyncBucketProxy>()
        every { bucketProvider.resolveBucket("user:1") } returns bucket
        every {
            bucket.tryConsumeAndReturnRemaining(5)
        } returns CompletableFuture.completedFuture(ConsumptionProbe.consumed(5, 0))

        val result = DistributedSuspendRateLimiter(bucketProvider).consume("user:1", 5)

        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.consumedTokens shouldBeEqualTo 5
        result.availableTokens shouldBeEqualTo 5

        verify(exactly = 1) { bucket.tryConsumeAndReturnRemaining(5) }
        verify(exactly = 0) { bucket.availableTokens }
    }
}

package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.coroutines.coAwait
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RateType
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class RateLimiterExamples: AbstractRedissonCoroutineTest() {

    companion object: KLogging() {
        private val defaultDuration = 100.seconds.toJavaDuration()
    }

    @Test
    fun `rate limiter 를 이용하여 요청 제한하기`() = runSuspendIO {
        val limiter = redisson.getRateLimiter(randomName())

        // 2초 동안 5개의 request 만 허용
        limiter.trySetRateAsync(RateType.OVERALL, 5, defaultDuration).coAwait()

        // 3개
        limiter.tryAcquireAsync(1).coAwait().shouldBeTrue()
        limiter.tryAcquireAsync(1).coAwait().shouldBeTrue()
        limiter.tryAcquireAsync(1).coAwait().shouldBeTrue()

        val job = scope.launch {
            // 2개
            limiter.tryAcquireAsync(1).coAwait().shouldBeTrue()
            limiter.tryAcquireAsync(1).coAwait().shouldBeTrue()
            yield()

            // 5개 모두 소진됨
            limiter.availablePermitsAsync().coAwait() shouldBeEqualTo 0L
            limiter.tryAcquireAsync(1).coAwait().shouldBeFalse()
        }
        yield()
        job.join()
        yield()

        // 5개 모두 소진됨
        limiter.availablePermitsAsync().coAwait() shouldBeEqualTo 0L
        limiter.tryAcquireAsync(1).coAwait().shouldBeFalse()

        limiter.deleteAsync().coAwait().shouldBeTrue()
    }

    @Test
    fun `RedissonClient 인스턴스 별로 rate limit 를 따로 허용한다 in multi threading`() {
        val limiterName = randomName()

        val limiter1 = redisson.getRateLimiter(limiterName)
        // 2초 동안 각 client 별로 3개의 request 만 허용
        limiter1.trySetRate(RateType.PER_CLIENT, 3, defaultDuration)

        // Redisson이 Initialize 할 시간이 필요함
        limiter1.acquire()
        limiter1.acquire()
        limiter1.acquire()
        Thread.sleep(10)
        limiter1.availablePermits() shouldBeEqualTo 0L
        limiter1.tryAcquire(1).shouldBeFalse()

        MultithreadingTester()
            .numThreads(2)
            .roundsPerThread(2)
            .add {
                val redisson = newRedisson()
                // RRateLimiter Exception----RateLimiter is not initialized
                // https://github.com/redisson/redisson/issues/2451
                val limiter2 = redisson.getRateLimiter(limiterName)
                limiter2.trySetRate(RateType.PER_CLIENT, 3, defaultDuration)
                    .shouldBeFalse()               // 이미 limiter1 에서 initialize 했으므로, false 를 반환한다
                Thread.sleep(1)

                try {
                    // limiter2는 3개 모두 소진
                    repeat(3) {
                        limiter2.tryAcquire(1).shouldBeTrue()
                    }
                    Thread.sleep(1)
                    // limiter2는 모두 소진됨
                    limiter2.availablePermits() shouldBeEqualTo 0L
                    limiter2.tryAcquire(1).shouldBeFalse()
                } finally {
                    redisson.shutdown()
                }
                Thread.sleep(1)
            }
            .run()

        limiter1.availablePermits() shouldBeEqualTo 0L
        limiter1.tryAcquire(1).shouldBeFalse()

        limiter1.delete().shouldBeTrue()
    }

    @Test
    fun `RedissonClient 인스턴스 별로 rate limit 를 따로 허용한다 in multi job`() = runSuspendIO {
        val limiterName = randomName()

        val limiter1 = redisson.getRateLimiter(limiterName)
        // 2초 동안 각 client 별로 3개의 request 만 허용
        limiter1.trySetRateAsync(RateType.PER_CLIENT, 3, defaultDuration).coAwait()
        // Redisson이 Initialize 할 시간이 필요함
        limiter1.acquireAsync().coAwait()
        limiter1.acquireAsync().coAwait()
        limiter1.acquireAsync().coAwait()
        delay(10)
        limiter1.availablePermitsAsync().coAwait() shouldBeEqualTo 0L
        limiter1.tryAcquireAsync(1).coAwait().shouldBeFalse()

        MultijobTester()
            .numThreads(2)
            .roundsPerJob(2)
            .add {
                // RateType 이 PER_CLIENT 인 경우, RedissonClient 인스턴스 별로 rate limit 를 따로 허용한다
                val redisson1 = newRedisson()
                try {
                    // RRateLimiter Exception----RateLimiter is not initialized
                    // https://github.com/redisson/redisson/issues/2451
                    val limiter2 = redisson1.getRateLimiter(limiterName)
                    limiter2.trySetRateAsync(RateType.PER_CLIENT, 3, defaultDuration)
                        .coAwait().shouldBeFalse()  // 이미 limiter1 에서 initialize 했으므로, false 를 반환한다
                    delay(1)

                    // limiter2는 3개 모두 소진
                    repeat(3) {
                        limiter2.tryAcquireAsync(1).coAwait().shouldBeTrue()
                    }
                    delay(1)
                    // limiter2는 모두 소진됨
                    limiter2.availablePermitsAsync().coAwait() shouldBeEqualTo 0L
                    limiter2.tryAcquireAsync(1).coAwait().shouldBeFalse()
                } finally {
                    redisson1.shutdown()
                }
                delay(1)
            }
            .run()

        limiter1.availablePermitsAsync().coAwait() shouldBeEqualTo 0L
        limiter1.tryAcquireAsync(1).coAwait().shouldBeFalse()

        limiter1.deleteAsync().coAwait().shouldBeTrue()
    }
}

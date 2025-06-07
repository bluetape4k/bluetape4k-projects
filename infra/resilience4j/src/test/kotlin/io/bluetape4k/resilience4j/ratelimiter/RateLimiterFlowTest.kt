package io.bluetape4k.resilience4j.ratelimiter

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.resilience4j.SuspendHelloWorldService
import io.github.resilience4j.kotlin.ratelimiter.rateLimiter
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class RateLimiterFlowTest {

    companion object: KLogging()

    private fun noWaitConfig() =
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(10))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ZERO)
            .build()

    @Test
    fun `rate limit에 걸리지 않을 때에는 flow를 수행됩니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = SuspendHelloWorldService()

        val testFlow = flow {
            emit(helloWorldService.returnHelloWorld())
        }
            .rateLimiter(rateLimiter)
            .single()

        testFlow shouldBeEqualTo "Hello world"
        metrics.availablePermissions shouldBeEqualTo 9
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `예외가 발샣하는 flow도 수행됩니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = SuspendHelloWorldService()

        assertFailsWith<IllegalStateException> {
            flow {
                emit(helloWorldService.throwException())
            }
                .rateLimiter(rateLimiter)
                .single()
        }

        metrics.availablePermissions shouldBeEqualTo 9
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `rate limit에 도달하고, 대기를 허용하지 않는 경우에는 flow를 실행하지 않습니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = SuspendHelloWorldService()

        repeat(10) {
            flow { emit(helloWorldService.returnHelloWorld()) }
                .rateLimiter(rateLimiter)
                .single()
        }

        assertFailsWith<RequestNotPermitted> {
            flow {
                emit(helloWorldService.returnHelloWorld())
            }
                .rateLimiter(rateLimiter)
                .single()
        }

        metrics.availablePermissions shouldBeEqualTo 0
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 10
    }
}

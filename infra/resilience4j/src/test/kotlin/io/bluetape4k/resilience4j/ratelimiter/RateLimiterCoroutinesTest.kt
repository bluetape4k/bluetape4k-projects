package io.bluetape4k.resilience4j.ratelimiter

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.resilience4j.CoHelloWorldService
import io.github.resilience4j.kotlin.ratelimiter.decorateSuspendFunction
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class RateLimiterCoroutinesTest {

    companion object: KLogging()

    private fun noWaitConfig() =
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(10))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ZERO)
            .build()

    @Test
    fun `rate limit에 걸리지 않을 때에는 method를 수행됩니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = CoHelloWorldService()

        val result = rateLimiter.executeSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        result shouldBeEqualTo "Hello world"
        metrics.availablePermissions shouldBeEqualTo 9
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `예외를 발생시키는 함수에 대해서도 실행되어야 합니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = CoHelloWorldService()

        assertFailsWith<IllegalStateException> {
            rateLimiter.executeSuspendFunction {
                helloWorldService.throwException()
            }
        }

        metrics.availablePermissions shouldBeEqualTo 9
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `rate limit이 꽉 찬 경우에는 실행되지 않습니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = CoHelloWorldService()

        repeat(10) {
            rateLimiter.executeSuspendFunction {
                helloWorldService.returnHelloWorld()
            }
        }

        assertFailsWith<RequestNotPermitted> {
            rateLimiter.executeSuspendFunction {
                helloWorldService.returnHelloWorld()
            }
        }

        metrics.availablePermissions shouldBeEqualTo 0
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 10
    }

    @Test
    fun `method를 decorate 합니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = CoHelloWorldService()

        val function = rateLimiter.decorateSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        function() shouldBeEqualTo "Hello world"
        metrics.availablePermissions shouldBeEqualTo 9
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `인자가 있는 method를 decorate 합니다`() = runSuspendTest {
        val rateLimiter = RateLimiter.of("testName", noWaitConfig())
        val metrics = rateLimiter.metrics
        val helloWorldService = CoHelloWorldService()

        val function = rateLimiter.decorateSuspendFunction {
            helloWorldService.returnMessage("Hello debop")
        }

        function() shouldBeEqualTo "Hello debop"
        metrics.availablePermissions shouldBeEqualTo 9
        metrics.numberOfWaitingThreads shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }
}

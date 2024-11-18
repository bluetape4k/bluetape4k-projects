package io.bluetape4k.resilience4j.ratelimiter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration

class RateLimiterExamples {

    companion object: KLogging()

    @Test
    fun `setup RateLimiter config`() {
        val config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMillis(10))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(50))
            .build()


        val registry = RateLimiterRegistry.of(config)

        // create RateLimiter with default config
        val defaultRateLimiter = RateLimiterRegistry.ofDefaults().rateLimiter("default")

        // create RateLimiter with custom config
        val customRateLimiter = registry.rateLimiter("custom")

        defaultRateLimiter.rateLimiterConfig shouldNotBeEqualTo config
        customRateLimiter.rateLimiterConfig shouldBeEqualTo config

        // create RateLimiter directly
        val rateLimiter1 = RateLimiter.ofDefaults("API")
        val rateLimiter2 = RateLimiter.of("directCustom", config)

        rateLimiter1.rateLimiterConfig shouldNotBeEqualTo config
        rateLimiter2.rateLimiterConfig shouldBeEqualTo config
    }

    @Test
    fun `use rate limiter`() {
        val rateLimiter = RateLimiter.ofDefaults("default")

        val runnable: () -> Unit = { log.debug { "doSomething" } }
        val checkedRun = rateLimiter.checkedRunnable(runnable)

        runCatching { checkedRun.run() }
            .mapCatching { checkedRun.run() }
            .onFailure {
                log.info { "재호출 하기 전에 대기합니다." }
            }
    }

    @Test
    fun `dynamic rate limiter reconfiguration`() {
        val rateLimiter = RateLimiter.ofDefaults("dynamic")

        val restrictCall = rateLimiter.checkedRunnable {
            log.info { "100 ms 정도 지연되고 예외가 발생합니다." }
            Thread.sleep(100)
            throw RuntimeException("Boom!")
        }

        // during second refresh cycle limiter will get 1 permissions
        rateLimiter.changeLimitForPeriod(100)
        rateLimiter.changeTimeoutDuration(Duration.ofMillis(1000))

        // 예외가 발생하므로, 두번째 호출은 실행되지 않습니다.
        runCatching { restrictCall.run() }
            .mapCatching { restrictCall.run() }
            .onFailure {
                log.info { "재호출 하기 전에 대기합니다." }
            }
    }

    @Test
    fun `monitoring rate limiter`() {
        val rateLimiter = RateLimiter.ofDefaults("default")

        val restrictCall = rateLimiter.runnable {
            log.info { "100 ms 정도 지연되고 예외가 발생합니다." }
            Thread.sleep(100)
            throw java.lang.RuntimeException("Boom!")
        }

        // during second refresh cycle limiter will get 1 permissions
        rateLimiter.changeLimitForPeriod(100)
        rateLimiter.changeTimeoutDuration(Duration.ofMillis(1000))

        runCatching { restrictCall() }
            .mapCatching { restrictCall() }
            .onFailure {
                log.info { "재호출 하기 전에 대기합니다." }
            }

        val metrics = rateLimiter.metrics
        val waitingThreads = metrics.numberOfWaitingThreads
        val availablePermission = metrics.availablePermissions

        log.debug { "waitingThread=$waitingThreads" }
        log.debug { "availablePermission=$availablePermission" }
    }
}

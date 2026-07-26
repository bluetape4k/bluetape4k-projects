package io.bluetape4k.ktor.resilience4j

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.ktor.core.ApiErrorResponse
import io.bluetape4k.ktor.core.Bluetape4kKtorCoreConfig
import io.bluetape4k.ktor.core.Bluetape4kKtorJson
import io.bluetape4k.ktor.core.bluetape4kErrorResponses
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorResilienceSupportTest {

    private val json = Bluetape4kKtorJson.defaultJson()

    @Test
    fun `resilientGet retries route handler and returns success`() = testApplication {
        val attempts = AtomicInteger()
        val retry = Retry.of(
            "ktor.route.retry",
            RetryConfig.custom<Any>()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(1))
                .build()
        )

        application {
            installResilienceTestStatusPages()
            routing {
                resilientGet("/unstable", KtorResiliencePolicies(retry = retry)) {
                    if (attempts.incrementAndGet() == 1) {
                        throw IllegalStateException("transient")
                    }
                    call.respondText("ok")
                }
            }
        }

        val response = client.get("/unstable")

        response.status shouldBeEqualTo HttpStatusCode.OK
        response.bodyAsText() shouldBeEqualTo "ok"
        attempts.get() shouldBeEqualTo 2
    }

    @Test
    fun `resilience status pages map open circuit to service unavailable`() = testApplication {
        val circuitBreaker = CircuitBreaker.ofDefaults("ktor.route.open")
        circuitBreaker.transitionToOpenState()

        application {
            installResilienceTestStatusPages()
            routing {
                resilientGet("/open", KtorResiliencePolicies(circuitBreaker = circuitBreaker)) {
                    call.respondText("unreachable-secret")
                }
            }
        }

        val response = client.get("/open")
        val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())

        response.status shouldBeEqualTo HttpStatusCode.ServiceUnavailable
        body.error shouldBeEqualTo "circuit_breaker_open"
        body.message shouldBeEqualTo "Service temporarily unavailable"
        body.path shouldBeEqualTo "/open"
    }

    @Test
    fun `resilience status pages map rate limiter rejection to too many requests`() = testApplication {
        val rateLimiter = RateLimiter.of(
            "ktor.route.rate",
            RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build()
        )

        application {
            installResilienceTestStatusPages()
            routing {
                resilientGet("/limited", KtorResiliencePolicies(rateLimiter = rateLimiter)) {
                    call.respondText("ok")
                }
            }
        }

        client.get("/limited").status shouldBeEqualTo HttpStatusCode.OK

        val response = client.get("/limited")
        val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())

        response.status shouldBeEqualTo HttpStatusCode.TooManyRequests
        body.error shouldBeEqualTo "rate_limited"
        body.message shouldBeEqualTo "Too many requests"
        body.path shouldBeEqualTo "/limited"
    }

    @Test
    fun `resilience status pages map time limiter timeout to gateway timeout`() = testApplication {
        val timeLimiter = TimeLimiter.of(
            "ktor.route.timeout",
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(10))
                .build()
        )

        application {
            installResilienceTestStatusPages()
            routing {
                resilientGet("/timeout", KtorResiliencePolicies(timeLimiter = timeLimiter)) {
                    delay(100)
                    call.respondText("late-secret")
                }
            }
        }

        val response = client.get("/timeout")
        val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())

        response.status shouldBeEqualTo HttpStatusCode.GatewayTimeout
        body.error shouldBeEqualTo "timeout"
        body.message shouldBeEqualTo "Request timed out"
        body.path shouldBeEqualTo "/timeout"
    }

    @Test
    fun `time limiter records ordinary handler failures`() = runSuspendIO {
        val timeLimiter = TimeLimiter.of(
            "ktor.route.failure",
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .build()
        )
        val errorEvents = AtomicInteger()
        val recordedFailure = AtomicReference<Throwable>()
        timeLimiter.eventPublisher.onError { event ->
            errorEvents.incrementAndGet()
            recordedFailure.set(event.throwable)
        }

        val thrown = assertFailsWith<IllegalStateException> {
            withKtorResilience(KtorResiliencePolicies(timeLimiter = timeLimiter)) {
                throw IllegalStateException("ordinary failure")
            }
        }

        thrown.message shouldBeEqualTo "ordinary failure"
        errorEvents.get() shouldBeEqualTo 1
        recordedFailure.get().shouldNotBeNull().message shouldBeEqualTo "ordinary failure"
    }

    @Test
    fun `time limiter rethrows cancellation without recording policy failure`() = runSuspendIO {
        val timeLimiter = TimeLimiter.of(
            "ktor.route.time-limiter.cancel",
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .build()
        )
        val errorEvents = AtomicInteger()
        timeLimiter.eventPublisher.onError { errorEvents.incrementAndGet() }

        assertFailsWith<CancellationException> {
            withKtorResilience(KtorResiliencePolicies(timeLimiter = timeLimiter)) {
                throw CancellationException("client disconnected")
            }
        }

        errorEvents.get() shouldBeEqualTo 0
    }

    @Test
    fun `cancellation is rethrown and not counted as circuit breaker failure`() = runSuspendIO {
        val circuitBreaker = CircuitBreaker.of(
            "ktor.route.cancel",
            CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(1)
                .slidingWindowSize(2)
                .failureRateThreshold(1.0f)
                .build()
        )

        assertFailsWith<CancellationException> {
            withKtorResilience(KtorResiliencePolicies(circuitBreaker = circuitBreaker)) {
                throw CancellationException("client disconnected")
            }
        }

        circuitBreaker.metrics.numberOfFailedCalls shouldBeEqualTo 0
        circuitBreaker.state shouldBeEqualTo CircuitBreaker.State.CLOSED
    }

    private fun io.ktor.server.application.Application.installResilienceTestStatusPages() {
        installBluetape4kKtorCore(
            Bluetape4kKtorCoreConfig(
                installStatusPages = false,
                installHealthRoutes = false
            )
        )
        install(StatusPages) {
            bluetape4kResilienceErrors()
            bluetape4kErrorResponses()
        }
    }
}

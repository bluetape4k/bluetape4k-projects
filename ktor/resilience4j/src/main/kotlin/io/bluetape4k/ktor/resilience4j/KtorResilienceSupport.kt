package io.bluetape4k.ktor.resilience4j

import io.bluetape4k.resilience4j.ratelimiter.withRateLimiter
import io.bluetape4k.resilience4j.retry.withRetry as withSuspendRetry
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.timelimiter.TimeLimiter
import io.ktor.http.HttpMethod
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingHandler
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Executes [block] through caller-owned Resilience4j policies.
 *
 * ## Contract
 * - Policies are opt-in and scoped to this invocation.
 * - Coroutine cancellation is rethrown and is not recorded as a policy failure.
 * - Composition order is TimeLimiter -> RateLimiter -> CircuitBreaker -> Retry,
 *   matching the existing `bluetape4k-resilience4j` facade guidance.
 */
suspend fun <T: Any> withKtorResilience(
    policies: KtorResiliencePolicies,
    block: suspend () -> T,
): T {
    if (policies.isEmpty) {
        return block()
    }

    var protectedBlock: suspend () -> T = { block() }

    policies.timeLimiter?.let { timeLimiter ->
        val current = protectedBlock
        protectedBlock = { withTimeLimiterPreservingStatusMapping(timeLimiter) { current() } }
    }
    policies.rateLimiter?.let { rateLimiter ->
        val current = protectedBlock
        protectedBlock = { withRateLimiter(rateLimiter) { current() } }
    }
    policies.circuitBreaker?.let { circuitBreaker ->
        val current = protectedBlock
        protectedBlock = { withCircuitBreakerPreservingCancellation(circuitBreaker) { current() } }
    }
    policies.retry?.let { retry ->
        val current = protectedBlock
        protectedBlock = {
            withSuspendRetry<T>(retry) {
                current()
            }
        }
    }

    return protectedBlock()
}

/**
 * Adds a route whose handler is protected by [policies].
 */
fun Route.resilientRoute(
    method: HttpMethod,
    path: String,
    policies: KtorResiliencePolicies,
    body: RoutingHandler,
): Route =
    route(path, method) {
        handle {
            withKtorResilience(policies) {
                body()
            }
        }
    }

/**
 * Adds a GET route protected by [policies].
 */
fun Route.resilientGet(
    path: String,
    policies: KtorResiliencePolicies,
    body: RoutingHandler,
): Route =
    get(path) {
        withKtorResilience(policies) {
            body()
        }
    }

/**
 * Adds a POST route protected by [policies].
 */
fun Route.resilientPost(
    path: String,
    policies: KtorResiliencePolicies,
    body: RoutingHandler,
): Route =
    post(path) {
        withKtorResilience(policies) {
            body()
        }
    }

internal suspend fun <T: Any> withCircuitBreakerPreservingCancellation(
    circuitBreaker: CircuitBreaker,
    block: suspend () -> T,
): T {
    if (!circuitBreaker.tryAcquirePermission()) {
        throw CallNotPermittedException.createCallNotPermittedException(circuitBreaker)
    }

    val start = circuitBreaker.currentTimestamp
    try {
        val result = block()
        circuitBreaker.onResult(
            circuitBreaker.currentTimestamp - start,
            circuitBreaker.timestampUnit,
            result
        )
        return result
    } catch (e: CancellationException) {
        circuitBreaker.releasePermission()
        throw e
    } catch (e: Throwable) {
        circuitBreaker.onError(
            circuitBreaker.currentTimestamp - start,
            circuitBreaker.timestampUnit,
            e
        )
        throw e
    }
}

/** 정책이 소유한 timeout만 변환하며 상위 및 핸들러 내부의 취소는 그대로 전파합니다. */
internal suspend fun <T: Any> withTimeLimiterPreservingStatusMapping(
    timeLimiter: TimeLimiter,
    block: suspend () -> T,
): T {
    return try {
        val result = withTimeoutOrNull(timeLimiter.timeLimiterConfig.timeoutDuration.toMillis()) {
            block()
        } ?: throw TimeLimiter.createdTimeoutExceptionWithName(timeLimiter.name, null)
        timeLimiter.onSuccess()
        result
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        timeLimiter.onError(e)
        throw e
    }
}

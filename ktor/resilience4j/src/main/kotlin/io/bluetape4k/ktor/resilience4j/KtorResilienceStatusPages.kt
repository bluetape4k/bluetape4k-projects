package io.bluetape4k.ktor.resilience4j

import io.bluetape4k.ktor.core.respondApiError
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import java.util.concurrent.TimeoutException

/**
 * Resilience4j route failure에 대한 generic JSON error response를 등록합니다.
 *
 * ## Contract
 * - Open circuit failures map to HTTP 503.
 * - Rate limiter rejections map to HTTP 429.
 * - Timeout failures map to HTTP 504.
 * - Messages are generic and safe for clients; policy names and internals are not exposed.
 */
fun StatusPagesConfig.bluetape4kResilienceErrors() {
    exception<CallNotPermittedException> { call, _ ->
        call.respondApiError(
            status = HttpStatusCode.ServiceUnavailable,
            error = "circuit_breaker_open",
            message = "Service temporarily unavailable"
        )
    }
    exception<RequestNotPermitted> { call, _ ->
        call.respondApiError(
            status = HttpStatusCode.TooManyRequests,
            error = "rate_limited",
            message = "Too many requests"
        )
    }
    exception<TimeoutException> { call, _ ->
        call.respondApiError(
            status = HttpStatusCode.GatewayTimeout,
            error = "timeout",
            message = "Request timed out"
        )
    }
}

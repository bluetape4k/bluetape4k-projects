package io.bluetape4k.ktor.resilience4j

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter

/**
 * Resilience4j policies applied to a Ktor route or suspend block.
 *
 * ## Contract
 * - Every policy is optional and caller-owned.
 * - Policy names come from the supplied Resilience4j objects, so Micrometer tags
 *   and registry naming remain application controlled.
 * - This class does not create global registries or hidden Ktor plugins.
 */
class KtorResiliencePolicies(
    val circuitBreaker: CircuitBreaker? = null,
    val retry: Retry? = null,
    val rateLimiter: RateLimiter? = null,
    val timeLimiter: TimeLimiter? = null,
) {
    val isEmpty: Boolean
        get() = circuitBreaker == null && retry == null && rateLimiter == null && timeLimiter == null
}

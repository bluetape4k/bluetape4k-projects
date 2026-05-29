# Issue 644 Ktor Resilience4j Design

## Context

Issue #644 asks for optional Ktor integration helpers for Resilience4j retry, circuit breaker, rate limiter, and timeout policies.

The initial Ktor foundation is now present:

- `bluetape4k-ktor-core`
- `bluetape4k-ktor-observability`
- `bluetape4k-ktor-testing`
- `idgenerator-ktor-demo` adoption from the module family epic

The existing `bluetape4k-resilience4j` module already owns coroutine-safe Resilience4j facade behavior. This Ktor module should not duplicate that facade.

## Decision

Add `ktor/resilience4j` as `bluetape4k-ktor-resilience4j`.

Expose only route-scoped and block-scoped helpers:

- `KtorResiliencePolicies`
- `withKtorResilience`
- `Route.resilientRoute`
- HTTP verb convenience helpers for GET and POST
- `StatusPagesConfig.bluetape4kResilienceErrors`

## Boundaries

- No global Ktor plugin.
- No registry or config-file binding.
- No retry/circuit/rate/timeout policy creation DSL beyond passing concrete Resilience4j objects.
- No authentication, logging, tracing, or OpenAPI integration.
- No client-specific facade in this issue.

## Safety Requirements

- Retry, circuit breaker, rate limiter, and time limiter are opt-in per block or route.
- `CancellationException` is rethrown and must not be recorded as a circuit breaker failure.
- Status mappings use generic messages and do not expose policy internals beyond stable error codes.
- Circuit breaker and rate limiter names remain in the caller-owned Resilience4j objects, preserving Micrometer naming.

## Review Notes

- Security: generic error responses only; no token/header handling.
- Ops/SRE: failure modes map to 503, 429, and 504.
- Structural: new module depends on `ktor/core` and `infra/resilience4j`; core remains independent.
- Kotlin: public KDoc in English and cancellation-first handling.
- Tests: success, retry, open circuit, rate limit, and cancellation paths.

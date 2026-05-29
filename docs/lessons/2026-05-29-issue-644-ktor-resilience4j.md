# Issue 644 Ktor Resilience4j

## Context

Issue #644 followed the Ktor module family epic and asked for optional Resilience4j integration after the first server-side Ktor extension points were proven.

## Decision

Add `bluetape4k-ktor-resilience4j` as a thin route-scoped module. Reuse the existing `bluetape4k-resilience4j` coroutine facade and keep policy objects caller-owned.

## Outcome

The module now provides:

- `KtorResiliencePolicies`
- `withKtorResilience`
- `resilientRoute`, `resilientGet`, `resilientPost`
- `bluetape4kResilienceErrors`

Status mappings are generic: open circuit -> 503, rate limited -> 429, timeout -> 504. Cancellation is rethrown and not counted as a circuit breaker failure.

## Verification

- `./gradlew :bluetape4k-ktor-resilience4j:test --no-configuration-cache`

## Future Guard

Keep this module route/block scoped. Do not add global Ktor plugins, registry creation, auth, tracing, or OpenAPI behavior here.

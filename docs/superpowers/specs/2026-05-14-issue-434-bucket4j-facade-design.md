# Issue 434 Bucket4j Facade Design

Date: 2026-05-14 Issue: #434 Module: `:bluetape4k-bucket4j`

## Context

`infra/bucket4j` is the bluetape4k token-bucket rate-limit facade. It should stay focused on Bucket4j-backed local and distributed token consumption, while
`infra/resilience4j` owns retry, circuit breaker, bulkhead, time limiter, cache, and decorator policy composition.

The module already has:

- local and distributed `RateLimiter` / `SuspendRateLimiter` implementations;
- `LocalBucketProvider`, `LocalSuspendBucketProvider`, `BucketProxyProvider`, and `AsyncBucketProxyProvider`;
- Lettuce and Redisson proxy-manager helpers;
- coroutine `SuspendLocalBucket` that uses `delay` instead of thread blocking;
- Redis-backed integration tests and README pairs.

The remaining gap is contract hardening: `RateLimitResult` still has a deprecated compatibility constructor, rejection diagnostics do not expose Bucket4j probe retry/reset timing, and docs do not yet make provider lifecycle, expiration, configuration replacement, and Bucket4j-vs-Resilience4j boundaries explicit enough.

This issue is allowed to make a documented public API break inside the current unreleased line: removing the deprecated `RateLimitResult(consumedTokens,
availableTokens)` compatibility constructor. The change must be called out in
`CHANGELOG.md`, and current repo references must be verified with `rg` before merge.

## Evidence

- Local catalog pins Bucket4j `8.18.0`.
- Bucket4j `ConsumptionProbe` exposes `isConsumed`, `remainingTokens`,
  `nanosToWaitForRefill`, and `nanosToWaitForReset`.
- Bucket4j `Bucket` and `AsyncBucketProxy` support
  `replaceConfiguration(BucketConfiguration, TokensInheritanceStrategy)`.
- Bucket4j `BandwidthBuilder` supports `id(String)` for stable bandwidth mapping during configuration replacement.
- Bucket4j Lettuce and Redisson docs show explicit expiration-after-write configuration via `ExpirationAfterWriteStrategy`.
- Bucket4j listener events are client-side for distributed buckets; aggregated metrics require caller-side aggregation.

## Goals

1. Keep `infra/bucket4j` as a focused Kotlin/JVM rate-limit facade over Bucket4j.
2. Stabilize `RateLimitResult` around explicit status-based factories and probe diagnostics.
3. Keep cancellation propagation deterministic for coroutine paths.
4. Document provider ownership, Redis key prefixing, expiration strategy, and async distributed support.
5. Document and test safe configuration replacement with identified bandwidth IDs.

## Non-goals

- No Bucket4j internals port.
- No KMP/Kotlin Native support.
- No new distributed backend dependency.
- No retry/circuit-breaker/fallback policy inside `infra/bucket4j`.
- No Spring Boot auto-configuration unless a separate concrete user gap is proven.

## Proposed Design

### Public Boundary

`bluetape4k-bucket4j` owns:

- token-bucket consumption by key;
- local and Redis-distributed Bucket4j bucket resolution;
- sync and coroutine `consume(key, numToken)` facades;
- neutral `RateLimitResult` diagnostics.

It does not own:

- retry/backoff policy;
- fallback behavior;
- circuit breaking;
- resilience decorator composition.

Callers may compose `RateLimitResult` with `bluetape4k-resilience4j`, but the Bucket4j module should not depend on resilience policy.

### Result Contract

Remove the deprecated `RateLimitResult(consumedTokens, availableTokens)`
constructor and require explicit factory or primary-constructor use.

Extend `RateLimitResult` with a nested `RateLimitDiagnostics` value instead of adding several top-level constructor fields. This limits `componentN()`/`copy()`
blast radius while still making probe data available.

- `nanosToWaitForRefill`: retry-after timing from `ConsumptionProbe` for rejected attempts.
- `nanosToWaitForReset`: full-reset timing from `ConsumptionProbe`.
- `rejectionReason`: module-owned enum for rejected results, initially
  `INSUFFICIENT_TOKENS`.
- `retryAfter`: derived nullable `java.time.Duration` for caller-friendly retry headers.

Do not expose `ConsumptionProbe` directly through public `RateLimitResult`. Both `RateLimitResult` and `RateLimitDiagnostics` must implement
`Serializable` and declare `serialVersionUID`.
`RateLimitRejectionReason` values are additive-only: future changes may append new reasons, but existing enum names must not be renamed or reordered.

Status semantics:

| Status     | `consumedTokens`      | `availableTokens` | `diagnostics.nanosToWaitForRefill` | `diagnostics.nanosToWaitForReset` | `diagnostics.rejectionReason` | `retryAfter`                                           |
|------------|-----------------------|-------------------|------------------------------------|-----------------------------------|-------------------------------|--------------------------------------------------------|
| `CONSUMED` | requested token count | remaining tokens  | `0`                                | probe reset nanos                 | `null`                        | `null`                                                 |
| `REJECTED` | `0`                   | remaining tokens  | probe refill nanos                 | probe reset nanos                 | `INSUFFICIENT_TOKENS`         | `Duration.ofNanos(nanosToWaitForRefill)` when positive |
| `ERROR`    | `0`                   | `0`               | `0`                                | `0`                               | `null`                        | `null`                                                 |

`RateLimitResult.error(cause)` should sanitize diagnostics for API consumers:
cap messages at 256 characters, redact URI credentials, avoid stack traces, and avoid falling back to raw `Throwable.toString()`. Detailed exception data belongs in logs, not in the result object.

### Coroutine Contract

`SuspendLocalBucket.consume` / `tryConsume` continue to use `delay` for waits.
`LocalSuspendRateLimiter` and `DistributedSuspendRateLimiter` must rethrow
`CancellationException` and convert only non-cancellation failures to
`RateLimitResult.error`.

`SuspendRateLimiter.consume` remains an immediate consume attempt; it does not wait for token refill. Waiting behavior stays on `SuspendLocalBucket` for local bucket operations.

`DistributedSuspendRateLimiter` should support a caller-configurable timeout for the underlying `AsyncBucketProxy` future. The interface method may use a limiter-scoped default timeout, and the concrete class may expose a per-call timeout overload. On timeout, return `RateLimitResult.error`; on coroutine cancellation, rethrow `CancellationException`. If cancellation happens while a Redis operation is in flight, token state may still be changed server-side; this must be documented.

HTTP `Retry-After` conversion is caller-owned. The module exposes exact nanosecond diagnostics; HTTP adapters should round up to the protocol unit they emit, commonly at least one second for positive retry delays.

### Distributed Provider Contract

`BucketProxyProvider` and `AsyncBucketProxyProvider` own key namespacing and Bucket4j proxy construction only. They do not own Redis client lifecycle, do not read token state during resolve, and should document that callers own Redis client shutdown.

Lettuce and Redisson helper docs should state that callers should set an
`ExpirationAfterWriteStrategy` for production Redis storage.

The default key prefix is a development-safe namespace, not an application isolation strategy. Production callers should pass an application/policy-specific prefix. Provider code should cap serialized bucket key size to avoid Redis memory amplification from unbounded user input.

Async distributed support is accepted through the existing
`AsyncBucketProxyProvider` plus `DistributedSuspendRateLimiter` path. A broader general-purpose suspend wrapper for every `AsyncBucketProxy` method is rejected for this issue because it would duplicate Bucket4j's async API and expand the public surface beyond rate limiting.

### Configuration Evolution

README examples should show burst plus sustained bandwidths with stable IDs:

- `id("burst")`
- `id("sustained")`

Tests should cover that bandwidth IDs survive the DSL and that a Bucket4j local bucket can replace configuration using `TokensInheritanceStrategy.PROPORTIONALLY`. Distributed `AsyncBucketProxy.replaceConfiguration(..., PROPORTIONALLY)` should also be covered on at least the existing Lettuce and Redisson integration paths when Testcontainers are available.

## Acceptance Criteria

- [ ] `RateLimitResult` deprecated compatibility constructor is removed.
- [ ] `RateLimitResult` exposes nested retry-after/reset diagnostics without exposing Bucket4j internals.
- [ ] `RateLimitResult` / diagnostics serialization policy is explicit and
  `serialVersionUID` is declared.
- [ ] `RateLimitResult.error` redacts/caps public error messages.
- [ ] Rejected probe conversion preserves `nanosToWaitForRefill` and
  `nanosToWaitForReset`.
- [ ] Coroutine cancellation propagation remains covered for local waiting and distributed async `await`; cancellation is not converted to
  `RateLimitResult.error`.
- [ ] Distributed suspend calls have a documented timeout contract.
- [ ] Configuration replacement with identified bandwidth IDs is tested.
- [ ] Distributed configuration replacement is tested through async proxy paths or an explicit Testcontainers blocker is recorded in the PR body and
  `docs/lessons/`.
- [ ] Bucket key size validation is tested.
- [ ] README English/Korean pairs document module boundary, local vs distributed examples, coroutine behavior, provider lifecycle/expiration, async distributed support, configuration replacement, and Bucket4j-vs-Resilience4j guidance.
- [ ] Targeted `:bluetape4k-bucket4j:test` passes.

## Review Notes

### Claude Code Opus Advisor

Artifact: `.omx/artifacts/claude-issue-434-bucket4j-spec-review-20260514-102314.md`

| Priority | Finding                                                                                        | Decision  | Follow-up                                                                                                                 |
|----------|------------------------------------------------------------------------------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------|
| P0       | Deprecated constructor removal and result-shape expansion need explicit binary-break handling. | Accepted. | Spec now documents the unreleased public break, repo-wide reference check, nested diagnostics, and CHANGELOG requirement. |
| P0       | Diagnostic nanos semantics are undefined.                                                      | Accepted. | Added status semantics table and `retryAfter` derivation.                                                                 |
| P0       | Distributed suspend `await()` has no timeout contract.                                         | Accepted. | Added limiter-scoped/per-call timeout requirement and cancellation-state note.                                            |
| P0       | `errorMessage` can leak raw exception data.                                                    | Accepted. | Added redaction/capping requirement.                                                                                      |
| P0       | Cancellation and distributed configuration replacement tests are missing.                      | Accepted. | Added explicit acceptance criteria.                                                                                       |
| P1       | Key prefix/key length and listener/observability boundaries need clarity.                      | Accepted. | Added provider key-size and production prefix guidance; README will document listener/aggregation ownership.              |
| P1       | Public KDoc language must be English.                                                          | Accepted. | New/changed public KDoc will be English per workspace AGENTS policy.                                                      |

### Claude Code Opus Advisor Re-review

Artifact: `.omx/artifacts/claude-issue-434-bucket4j-spec-rereview-20260514-102705.md`

Result: P0=0, P1=0. Accepted P2 clarifications for reset-nanos semantics, Testcontainers blocker location, HTTP retry-after rounding ownership, 256-char redaction cap, and additive-only rejection-reason evolution.

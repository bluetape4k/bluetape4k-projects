# Issue 435 Resilience4j Facade Design

Date: 2026-05-14 Issue: #435 Module: `:bluetape4k-resilience4j`

## Context

`infra/resilience4j` is the bluetape4k coroutine facade for Resilience4j fault tolerance policies. It should stay focused on policy composition and coroutine ergonomics:

- circuit breaker, retry, rate limiter, bulkhead, time limiter, cache, fallback;
- suspend function and Flow integration;
- upstream registries, events, Micrometer, and Spring Boot configuration compatibility.

`infra/bucket4j` owns token-bucket rate-limit mechanics, quota diagnostics, and distributed bucket storage. This issue must keep that boundary explicit so Resilience4j rate limiter guidance does not imply Bucket4j behavior.

The module already has suspend wrappers for the Resilience4j Kotlin extensions,
`SuspendDecorators`, Flow tests, cache wrappers, and README locale pairs. The remaining gap is contract hardening: cancellation semantics are not uniformly tested across the public facade, composition order is under-specified, several public KDocs are still Korean, and cache miss handling still has weak cancellation/error contracts.

## Evidence

- Local catalog pins Resilience4j `2.4.0`.
- Resilience4j Kotlin docs provide suspend extensions for `CircuitBreaker`,
  `RateLimiter`, `Retry`, `TimeLimiter`, and semaphore `Bulkhead`.
- Upstream Kotlin rate limiter and retry suspend extensions use coroutine
  `delay()` where waiting is needed and do not change the coroutine context.
- Upstream TimeLimiter suspend extensions use `withTimeout`; timeout raises
  `TimeoutCancellationException`, cancels the coroutine, and ignores
  `cancelRunningFuture`.
- Upstream Kotlin Bulkhead docs warn that non-zero `maxWaitTime` can block while waiting for permission.
- Resilience4j decorators can stack policies and fallback. Upstream registries, event publishers, and Micrometer integrations remain the source of truth for lifecycle and observability.
- Current `SuspendDecorators` composes by wrapping the current function at each
  `withXxx` call; the last `withXxx` call becomes the outermost decorator invoked first.
- Current `SuspendCacheImpl.rawGetWithHit`, `getValueFromCache`, and
  `putValueIntoCache` catch `Throwable` and can swallow coroutine cancellation when a JCache provider throws `CancellationException`.
- Resilience4j `Cache<K, V>` 2.4.0 exposes `computeIfAbsent`, metrics, name, and event publisher only. `javap` and the `resilience4j-cache-2.4.0` source jar show no public backing `javax.cache.Cache` accessor. Therefore
  `Cache.executeSuspendFunction` cannot be rewritten to explicit
  `containsKey`/`get` against the public upstream interface without reflection or a new API.
- Current `Cache.executeSuspendFunction` performs a nullable
  `computeIfAbsent(key) { null }` probe. Because no upstream backing-cache accessor exists, this issue keeps that public `Cache<K, V>` extension as a best-effort compatibility path and directs strict JCache read/write semantics to `SuspendCache.of(jcache)`.

## Goals

1. Define `bluetape4k-resilience4j` as a coroutine-first facade over upstream Resilience4j policies, not a replacement for upstream engines.
2. Make cancellation propagation deterministic across suspend wrappers, fallback helpers, cache wrappers, and composed decorators.
3. Document and test `SuspendDecorators` composition order, fallback rules, and rejection/timeout behavior.
4. Make Flow operator semantics explicit: each existing upstream Flow operator protects the collection execution according to the upstream Kotlin module;
   `SuspendDecorators` itself remains suspend-function-only.
5. Keep observability and configuration ownership with upstream registries, event publishers, Micrometer, and Spring Boot auto-configuration.
6. Refresh README pairs with boundary, coroutine contracts, composition order, Flow semantics, and Bucket4j-vs-Resilience4j rate limiter guidance.

## Non-goals

- No Resilience4j internals port.
- No replacement of upstream registries, Spring Boot auto-configuration, or Micrometer integration.
- No ThreadPoolBulkhead coroutine facade.
- No distributed token-bucket or quota storage in this module.
- No KMP/Kotlin Native support.
- No dependency upgrade beyond the current version catalog.

## Proposed Design

### Public Boundary

`bluetape4k-resilience4j` owns:

- Kotlin-friendly `withXxx` and `decorateSuspendFunctionX` helpers;
- `SuspendDecorators` builder composition for suspend functions;
- coroutine-safe fallback helpers;
- `SuspendCache` wrapper for JCache-backed suspend loaders;
- README/KDoc guidance for policy composition and coroutine behavior.

It does not own:

- token-bucket storage, quota diagnostics, distributed Redis bucket state, or HTTP retry-after calculation;
- upstream registry lifecycle;
- Spring Boot property binding;
- Micrometer meter naming except where bluetape4k adds cache-specific behavior.

Use Resilience4j `RateLimiter` for simple policy composition around service calls. Use `bluetape4k-bucket4j` when callers need token-bucket quota semantics, distributed buckets, remaining-token diagnostics, or retry-after timing derived from bucket probes.

### Coroutine Cancellation Contract

All bluetape4k suspend facade paths must propagate `CancellationException`
unchanged:

- `withCircuitBreaker`, `withRetry`, `withRateLimiter`, `withBulkhead`,
  `withTimeLimiter`;
- one-argument and two-argument `decorateSuspendFunction` helpers;
- `SuspendDecorators` fallback overloads;
- `SuspendCache.computeIfAbsent`;
- Resilience4j `Cache.executeSuspendFunction` and `withCache`;
- representative Flow operators for circuit breaker, retry, rate limiter, bulkhead, and time limiter.

Fallback handlers must never recover coroutine cancellation, even when the caller passes `CancellationException::class` or `Throwable::class`. Retry must not retry cancellation. Cache wrappers must not convert cancellation into a cache miss or an error event.
`SuspendCacheImpl.rawGetWithHit`, `getValueFromCache`, and `putValueIntoCache`
must rethrow cancellation before returning null, logging, or publishing an error event.

`TimeoutCancellationException` from Resilience4j Kotlin TimeLimiter is an intentional timeout result, but it is still a `CancellationException` subtype. The module must document that callers who want fallback-on-timeout should place fallback outside TimeLimiter deliberately and should avoid matching all
`CancellationException` values. For this issue, bluetape4k keeps timeout propagation unchanged rather than converting it to `TimeoutException`.

### Decorator Composition

`SuspendDecorators` mutates the current function at each `withXxx` call:

```text
SuspendDecorators.ofSupplier(block)
    .withCircuitBreaker(cb)
    .withRetry(retry)
    .withFallback(handler)
```

This produces:

```text
fallback(retry(circuitBreaker(block)))
```

The last `withXxx` call is invoked first and sees failures from all earlier inner decorators. Recommended default order for service calls is:

```text
withBulkhead -> withTimeLimiter -> withRateLimit -> withCircuitBreaker -> withRetry -> withFallback
```

Resulting invocation:

```text
fallback(retry(circuitBreaker(rateLimiter(timeLimiter(bulkhead(block))))))
```

This default makes fallback outermost, retry observe inner policy exceptions, circuit breaker record each retry attempt outcome, rate limiter gate attempts, TimeLimiter bound each attempt, and Bulkhead limit concurrent execution near the protected call.

Callers may choose a different order when their policy requires it. The README must explain that ordering is part of the contract, not just style.

### Cache Contract

`SuspendCache.computeIfAbsent` should:

- reject null keys through Kotlin type usage and runtime validation where platform types can pass null;
- serialize concurrent misses per key;
- call the loader exactly once for concurrent misses on the same key;
- propagate loader cancellation unchanged;
- propagate JCache cancellation unchanged;
- publish error events only for non-cancellation cache failures;
- keep JCache access on caller-compatible paths without adding a new dependency.

`Cache.executeSuspendFunction` is constrained by the public Resilience4j
`Cache<K, V>` interface. It should:

- avoid reflection into `io.github.resilience4j.cache.internal.CacheImpl`;
- keep the current non-blocking two-phase suspend-loader shape instead of wrapping the suspend loader in `runBlocking` inside upstream
  `computeIfAbsent`;
- re-check coroutine cancellation after blocking cache probes;
- keep mutex release deterministic;
- document that strict direct JCache read/write semantics are available through
  `SuspendCache.of(jcache)`, not through the upstream `Cache<K, V>` facade.

### Flow Semantics

The existing Flow integration delegates to Resilience4j Kotlin operators. README must state the module contract in terms callers can rely on:

- Flow decoration is applied when a collection runs.
- A new collection re-enters the configured policy.
- Operators do not cache emitted elements unless the caller adds caching.
- Cancellation by downstream collectors propagates unchanged.
- TimeLimiter timeout cancels the collecting coroutine according to upstream Kotlin TimeLimiter behavior.
- Bulkhead with non-zero wait can block while acquiring permission; prefer zero wait for coroutine-heavy paths unless a bounded blocking wait is intentional.

### Events, Metrics, and Spring Configuration

Upstream Resilience4j registries remain the source of truth for instances, events, metrics, health indicators, and Spring properties. bluetape4k should document how to attach upstream `eventPublisher` consumers and should not invent a parallel metric naming scheme.

The only module-owned observability surface is `SuspendCache.metrics` and
`SuspendCache.eventPublisher`, which mirror cache hit/miss/error behavior for the JCache-backed suspend wrapper.

## Acceptance Criteria

- [ ] Public boundary for `infra/resilience4j` is documented in English and Korean README files.
- [ ] Public KDoc touched by this issue is English and documents cancellation behavior where relevant.
- [ ] Cancellation propagation tests cover each suspend wrapper category:
  circuit breaker, retry, rate limiter, bulkhead, time limiter, cache, and composed decorators.
- [ ] Fallback tests prove cancellation is not swallowed even when the requested fallback type is broad.
- [ ] Retry tests prove cancellation is not retried.
- [ ] `SuspendDecorators` composition order is documented and covered by focused tests.
- [ ] README `SuspendDecorators` examples use the recommended composition order and explicitly state that the last `withXxx` call is outermost.
- [ ] Flow operator semantics are documented and cancellation behavior is tested for representative operators.
- [ ] `Cache.executeSuspendFunction` documents its upstream-interface constraint and keeps cancellation checks around the compatibility path.
- [ ] Cache wrappers rethrow cancellation and publish error events only for non-cancellation failures.
- [ ] New suspend cancellation tests use `assertFailsWith<CancellationException>`
  or equivalent direct exception assertions, not `runCatching {}` around suspend calls.
- [ ] Events/metrics guidance points to upstream surfaces and documents only bluetape4k-added cache behavior.
- [ ] Targeted `:bluetape4k-resilience4j:test` passes.

## Review Notes

### Claude Code Opus Advisor

Artifact: `.omx/artifacts/claude-you-are-claude-code-opus-acting-as-an-external-advisor-for-a-2026-05-14T02-18-45-166Z.md`

| Priority | Finding                                                                                                                            | Decision                         | Follow-up                                                                                                                                                                |
|----------|------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| P0       | `rawGetWithHit` can swallow cancellation.                                                                                          | Accepted.                        | Spec now names `rawGetWithHit`; plan requires `rethrowIfCancellation()`.                                                                                                 |
| P0       | `getValueFromCache` can publish cancellation as cache error.                                                                       | Accepted.                        | Spec/plan now require rethrow before logging/events.                                                                                                                     |
| P0       | `Cache.executeSuspendFunction` asked for `containsKey`/`get`, but Resilience4j `Cache<K,V>` has no public backing JCache accessor. | Accepted with design adjustment. | Primary evidence recorded; strict JCache semantics stay on `SuspendCache`; public `Cache<K,V>` extension remains compatibility path without reflection or `runBlocking`. |
| P0       | README composition examples may preserve the old order.                                                                            | Accepted.                        | README acceptance criterion and plan task now require the spec-recommended order.                                                                                        |
| P1       | KDoc language rule must be explicit.                                                                                               | Accepted.                        | Public KDoc touched by this issue is English.                                                                                                                            |
| P1       | Existing tests use `runCatching` around suspend calls and miss `@TestInstance(PER_CLASS)`.                                         | Accepted.                        | Plan requires direct exception assertions and JUnit lifecycle annotations for touched/new tests.                                                                         |

Latest integrated review status: `P0 = 0`, `P1 = 0`.

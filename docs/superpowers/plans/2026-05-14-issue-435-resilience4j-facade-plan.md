# Issue 435 Resilience4j Facade Plan

Date: 2026-05-14
Issue: #435
Spec: `docs/superpowers/specs/2026-05-14-issue-435-resilience4j-facade-design.md`
Module: `:bluetape4k-resilience4j`

## Task Plan

### T1. Baseline Contract Tests

- Add a focused cancellation contract test file for suspend wrappers:
  `Resilience4jCancellationContractTest`.
- Cover `withCircuitBreaker`, `withRetry`, `withRateLimiter`, `withBulkhead`,
  `withTimeLimiter`, one-argument decorators, and two-argument decorators where
  existing wrappers differ.
- Assert `CancellationException` instance/type propagates and side effects show
  retry did not re-execute cancellation.
- Add `SuspendDecoratorsCancellationTest` for fallback overloads:
  - generic fallback;
  - `Throwable::class` fallback;
  - `CancellationException::class` fallback;
  - result fallback remains success-only.
- New suspend cancellation tests must use
  `assertFailsWith<CancellationException>` or direct exception assertions.
  Do not use `runCatching {}` around suspend calls in new cancellation tests.
- Add `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` to new or touched test
  classes with lifecycle methods, including existing `SuspendDecoratorsTest` if
  it is modified.

Validation:

- Run the new tests first and confirm any failures before implementation.
- Keep assertions on bluetape4k assertion helpers and `assertFailsWith`.

### T2. Cache Contract Hardening

- Update `Cache.executeSuspendFunction` to:
  - validate the key with `requireNotNull`;
  - avoid reflection into Resilience4j internal `CacheImpl`;
  - avoid `runBlocking` inside upstream `computeIfAbsent`;
  - preserve the two-phase suspend-loader compatibility path because
    Resilience4j `Cache<K, V>` 2.4.0 has no public backing JCache accessor;
  - re-check coroutine cancellation after blocking cache probes;
  - always release per-key mutex;
  - rethrow `CancellationException`.
- Update `SuspendCacheImpl` to:
  - call `e.rethrowIfCancellation()` in `rawGetWithHit`;
  - rethrow `CancellationException` from JCache access before logging;
  - call `e.rethrowIfCancellation()` in `getValueFromCache` and
    `putValueIntoCache`;
  - publish cache error events only for non-cancellation failures;
  - replace `cacheKey!!` event construction with validated non-null keys.
- Keep existing metrics behavior unless tests expose a regression.
- Use `kotlin.coroutines.cancellation.CancellationException` in new and touched
  Kotlin files.

Validation:

- Extend `SuspendCacheImplStabilityTest`.
- Add a `CacheCoroutinesCancellationTest` or extend existing cache tests for
- `io.github.resilience4j.cache.Cache<K, V>.executeSuspendFunction`
  cancellation around the compatibility path, plus `SuspendCacheImpl` loader
  and JCache-access cancellation.
- Run affected cache tests before full module test.

### T3. Decorator Composition Contract

- Add tests proving builder composition order:
  - last `withXxx` call is outermost;
  - fallback after retry observes retry exhaustion;
  - fallback before retry is retried when it throws;
  - circuit breaker/retry ordering is explicit in side-effect traces.
- Add KDoc to `SuspendDecorators` and nested builder classes in English.
- Public API KDoc touched by this issue follows the repo `AGENTS.md`/memory
  rule: English public KDoc and contributor artifacts. This takes precedence
  over older local guidance that asked for Korean KDoc.
- Keep deprecated `decoreate()` aliases unchanged for this issue, but leave
  English deprecation text when touched.

Validation:

- `SuspendDecoratorsTest` or a new focused order test passes.
- Grep README examples against actual method names.

### T4. Flow Semantics Coverage

- Add representative cancellation tests for Flow operators:
  - circuit breaker collection cancellation;
  - retry flow cancellation is not retried;
  - rate limiter or time limiter cancellation propagates.
- Avoid duplicating upstream library internals; test only the bluetape4k
  documented contract and existing wrapper usage.

Validation:

- Existing Flow tests plus new cancellation tests pass.

### T5. Documentation Refresh

- Update `infra/resilience4j/README.md`.
- Update `infra/resilience4j/README.ko.md`.
- Cover:
  - module boundary and Bucket4j-vs-Resilience4j rate limiter choice;
  - cancellation contract and TimeLimiter timeout behavior;
  - recommended `SuspendDecorators` composition order;
  - fallback rules;
  - Flow semantics;
  - upstream registry/event/Micrometer/Spring ownership;
  - cache-specific metrics/events.
- Replace `SuspendDecorators` examples with the spec-recommended order:
  `withBulkhead -> withTimeLimiter -> withRateLimit -> withCircuitBreaker ->
  withRetry -> withFallback`, and state that the last `withXxx` call is
  outermost.
- Keep README examples aligned with actual source symbols.

Validation:

- `rg` README-mentioned source symbols in `infra/resilience4j/src/main/kotlin`.
- Confirm English/Korean README sections stay synchronized.

### T6. Verification and Review

- Run `git diff --check`.
- Run IDE diagnostics when available for touched Kotlin files.
- Run targeted compile:
  `./gradlew :bluetape4k-resilience4j:compileTestKotlin --no-configuration-cache`.
- Run targeted tests:
  `./gradlew :bluetape4k-resilience4j:test --no-configuration-cache --rerun-tasks`.
- Run Kover if available:
  `./gradlew :bluetape4k-resilience4j:koverVerify :bluetape4k-resilience4j:koverXmlReport --no-configuration-cache`.
- Check whether a module detekt task exists; run it if present and record the
  gap if absent.
- Run Tier 4/Tier 5 code review locally and Claude Code Opus advisor review for
  the final diff.
- Capture `docs/lessons/2026-05-14-issue-435-resilience4j-facade.md`.

### T7. PR, CI, Merge, and Umbrella Close

- Commit with Lore trailers.
- Push feature branch and create a PR against `develop`.
- Assign `debop`.
- Use existing labels first: `enhancement`, `design`, `refactor`.
- Include verification evidence and Claude review artifact in the PR body.
- Wait for CI with an initial 5-minute wait before polling shorter intervals.
- Merge only after required checks pass.
- Sync local `develop`, remove the feature worktree/branch, and prune.
- Confirm #434 and #435 are closed, then close #433 with a comment linking both
  module-specific completions.

## Risks

- `TimeoutCancellationException` is both a timeout signal and a
  `CancellationException` subtype. The design keeps upstream behavior and
  documents it instead of adding a lossy conversion layer.
- Bulkhead non-zero wait can block. The README must warn coroutine callers
  rather than hiding the upstream behavior.
- Cache wrappers interact with JCache providers that differ in null handling.
  Replacing `computeIfAbsent` probing with explicit read/write reduces provider
  ambiguity.

## Review Notes

### Claude Code Opus Advisor

Artifact: `.omx/artifacts/claude-you-are-claude-code-opus-acting-as-an-external-advisor-for-a-2026-05-14T02-18-45-166Z.md`

| Priority | Finding | Decision | Follow-up |
| --- | --- | --- | --- |
| P0 | `rawGetWithHit` not named in T2. | Accepted. | T2 now names the function and required cancellation guard. |
| P0 | Resilience4j `Cache<K,V>` backing accessor missing. | Accepted with design adjustment. | T2 now records the no-accessor evidence and rejects reflection/`runBlocking`; strict path is `SuspendCache`. |
| P0 | README decorator examples need concrete replacement task. | Accepted. | T5 now names the exact recommended order. |
| P1 | New tests can repeat `runCatching` and lifecycle mistakes. | Accepted. | T1 now forbids `runCatching` for cancellation tests and requires lifecycle annotation where needed. |
| P1 | KDoc language conflict should be explicit. | Accepted. | T3 now pins English public KDoc for touched APIs. |

Latest integrated review status: `P0 = 0`, `P1 = 0`.

# Lessons Learned - Issue #831 Ktor TimeLimiter Failure Metrics (2026-06-24)

Related issue: #831
Module: `:bluetape4k-ktor-resilience4j`

## L1: TimeLimiter cancellation and failure accounting need separate catch paths

### Problem

`withTimeLimiterPreservingStatusMapping()` converted real timeouts to
Resilience4j `TimeoutException` and recorded success events, but ordinary
handler failures escaped without calling `timeLimiter.onError(e)`.

That made a TimeLimiter-protected Ktor invocation invisible to
Resilience4j TimeLimiter events when the handler failed with a normal
non-cancellation exception.

### Lesson

For coroutine TimeLimiter wrappers, keep the catch order explicit:

1. Convert `TimeoutCancellationException` to the TimeLimiter timeout exception
   and record it with `onError`.
2. Rethrow other `CancellationException` values without recording policy
   failure.
3. Record ordinary non-cancellation failures with `onError` before rethrowing.

The TimeLimiter public API exposes events for this module's verification
surface, so regression tests should assert `eventPublisher.onError` rather
than looking for CircuitBreaker-style metrics that TimeLimiter does not expose.

## Evidence

- RED: `time limiter records ordinary handler failures` failed because the
  TimeLimiter error event count stayed at 0.
- GREEN: `KtorResilienceSupportTest` passed with 7 tests.
- Module verification passed:
  `./gradlew :bluetape4k-ktor-resilience4j:compileKotlin :bluetape4k-ktor-resilience4j:compileTestKotlin :bluetape4k-ktor-resilience4j:test --no-build-cache`.
*** Add File: /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/fix-ktor-timelimiter-failures-831/docs/review/2026-06-24-issue-831-ktor-timelimiter-failures-review.md
# Review - Issue #831 Ktor TimeLimiter Failure Metrics

Date: 2026-06-24
Branch: `fix/ktor-timelimiter-failures-831`
Module: `:bluetape4k-ktor-resilience4j`

## Scope

- `ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceSupport.kt`
- `ktor/resilience4j/src/test/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceSupportTest.kt`

## Local Review

- P0/P1 findings: 0
- Catch ordering preserves timeout mapping:
  `TimeoutCancellationException` is handled before generic
  `CancellationException`.
- External coroutine cancellation remains unrecorded as a policy failure.
- Ordinary non-cancellation failures now call `timeLimiter.onError(e)` before
  being rethrown.
- Tests use existing `runSuspendIO` and bluetape4k assertions.
- No ad hoc concurrency stress helper was needed because this bug is a
  deterministic exception-boundary regression, not a race or contention issue.

## Validation Evidence

- RED:
  `./gradlew :bluetape4k-ktor-resilience4j:test --tests 'io.bluetape4k.ktor.resilience4j.KtorResilienceSupportTest.time limiter records ordinary handler failures' --no-build-cache`
  failed with `Expected <0> to equal to <1>`.
- GREEN:
  `./gradlew :bluetape4k-ktor-resilience4j:test --tests 'io.bluetape4k.ktor.resilience4j.KtorResilienceSupportTest' --no-build-cache`
  passed with 7 tests.
- Module:
  `./gradlew :bluetape4k-ktor-resilience4j:compileKotlin :bluetape4k-ktor-resilience4j:compileTestKotlin :bluetape4k-ktor-resilience4j:test --no-build-cache`
  passed.
- Static:
  `git diff --check` passed.

## Residual Risk

- Full repository build was not run; the change is isolated to the
  `ktor/resilience4j` TimeLimiter exception boundary and covered by module
  compile/test.

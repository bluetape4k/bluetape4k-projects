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

## Native Reviewer

- Reviewer: `code-reviewer`
- Verdict: APPROVE
- P0/P1 findings: 0
- Evidence: reviewer confirmed the catch order preserves timeout mapping,
  rethrows `CancellationException` before broad `Throwable`, and records
  ordinary failures with `timeLimiter.onError(e)`.

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

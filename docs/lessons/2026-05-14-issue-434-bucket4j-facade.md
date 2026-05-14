# Issue 434 Bucket4j Facade Hardening

## Context

Issue #434 hardened `infra/bucket4j` as a focused Kotlin/JVM Bucket4j facade.
The risk was broad API drift: `RateLimitResult` needed stable retry diagnostics,
distributed suspend behavior needed explicit cancellation/timeout semantics, and
distributed providers needed safer key/policy lifecycle documentation.

## Decision

- Keep the module boundary narrow: token-bucket rate limiting only; retry,
  timeout policy composition, circuit breakers, bulkheads, and fallback stay in
  Resilience4j.
- Make `RateLimitResult` own stable bluetape4k diagnostics instead of exposing
  Bucket4j probe types.
- Enforce a 512-byte prefixed bucket key limit before local or distributed
  bucket resolution.
- Preserve Java ergonomics for `DistributedSuspendRateLimiter` with
  `@JvmOverloads` while adding an optional async-store timeout.

## Outcome

- `RateLimitDiagnostics`, `RateLimitRejectionReason`, and `retryAfter` now cover
  rejection diagnostics.
- The deprecated `RateLimitResult(consumedTokens, availableTokens)` constructor
  was removed and the deprecated inventory was updated.
- README files now document provider lifecycle, Redis expiration ownership,
  bandwidth IDs for configuration replacement, async support, and module
  boundary.

## Verification

- `./gradlew :bluetape4k-bucket4j:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-bucket4j:test --no-configuration-cache` (121 tests)
- `./gradlew :bluetape4k-bucket4j:koverVerify --no-configuration-cache`
- Claude review reported no P0/P1 findings; low-risk P2 redaction, docs, and
  key-boundary test suggestions were applied.

## Future Agent Notes

- Do not add retry or circuit-breaker policy behavior to this module; document
  integration points and keep those policies in Resilience4j.
- When changing Bucket4j configuration replacement examples, keep stable
  `Bandwidth.id(...)` values in both English and Korean README files.
- When adding public result fields to data classes, check Kotlin `copy` /
  `componentN` and Java constructor compatibility explicitly.

# Issue #821 MongoDB Transaction Abort Review

Date: 2026-07-04
Repo: `bluetape4k-projects`
Scope: `data/mongodb` coroutine transaction cancellation cleanup

## Gate

- P0: 0
- P1: 0
- Verdict: PASS

## 7-Tier Review

### Tier 1 - Correctness

- PASS. `MongoClient.inTransaction` now aborts the MongoDB transaction from a
  `NonCancellable` context when the transaction block is cancelled.
- PASS. Abort failures are added as suppressed exceptions to the owner throwable
  and the original cancellation path is still rethrown.

### Tier 2 - API and Compatibility

- PASS. No public API signature or behavior was changed for successful
  transactions.
- PASS. The helper is private to the extension file and does not introduce a new
  user-facing contract.

### Tier 3 - Security and Privacy

- PASS. No credential, query, document, or logging data handling changed.
- PASS. Abort failure logging continues to log only the failure object and a
  fixed message.

### Tier 4 - Kotlin and bluetape4k Patterns

- PASS. The implementation rethrows `CancellationException` after cleanup and
  uses `NonCancellable` only for the suspend cleanup call.
- PASS. Tests use class-level MockK fields and reset them in `@BeforeEach` with
  `clearMocks(...)`.
- PASS. Assertions use bluetape4k assertion helpers and infix comparison style.

### Tier 5 - Tests

- PASS. Added regression coverage proving cancelled-context abort cleanup still
  reaches code after `yield()`.
- PASS. Added cancellation-path coverage for preserving abort failure details as
  suppressed exceptions.

### Tier 6 - Operations

- PASS. No dependency catalog, workflow, module registration, or CI path changes
  were required.

### Tier 7 - Documentation and Evidence

- PASS. This review and the issue lesson record the cancellation cleanup rule
  and verification evidence.

## Verification Evidence

- Compile validation:
  `./gradlew :bluetape4k-mongodb:compileKotlin :bluetape4k-mongodb:compileTestKotlin --no-build-cache --no-configuration-cache` passed.
- Targeted regression validation:
  `./gradlew :bluetape4k-mongodb:test --tests "io.bluetape4k.mongodb.MongoClientSupportTest" --no-build-cache --no-configuration-cache` passed with 12 tests.
- Module validation:
  `./gradlew :bluetape4k-mongodb:test --no-build-cache --no-configuration-cache` passed with 50 tests.
- Coverage report:
  `./gradlew :bluetape4k-mongodb:koverXmlReport --no-build-cache --no-configuration-cache` passed and generated `data/mongodb/build/reports/kover/report.xml`.
- `git diff --check` passed.

## Residual Risk

- The regression uses MockK to force a cancelled coroutine context at the
  transaction boundary. Real-driver transaction success and failure paths remain
  covered by the existing MongoDB module tests.

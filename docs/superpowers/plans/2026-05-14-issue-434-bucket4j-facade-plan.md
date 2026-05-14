# Issue 434 Bucket4j Facade Plan

Date: 2026-05-14
Issue: #434
Spec: `docs/superpowers/specs/2026-05-14-issue-434-bucket4j-facade-design.md`
Module: `:bluetape4k-bucket4j`

## Task Plan

### T1a. Result Constructor Cleanup

- Remove deprecated secondary `RateLimitResult(consumedTokens, availableTokens)`
  constructor.
- Update `CHANGELOG.md` for the breaking constructor removal.

Validation:

- Remove deprecated-constructor tests.
- Confirm no production/test references to removed constructor remain.
- Grep dependent examples/workshops for `RateLimitResult(`, `component`, and
  `copy(` callers.

### T1b. Result Diagnostics Contract

- Add `RateLimitRejectionReason`.
- Add nested `RateLimitDiagnostics` with `nanosToWaitForRefill`,
  `nanosToWaitForReset`, and rejection reason.
- Make `RateLimitDiagnostics` implement `Serializable`.
- Add derived `retryAfter` on `RateLimitResult`.
- Add `serialVersionUID` to result and diagnostics data classes.
- Add `RateLimitDiagnostics.EMPTY` to avoid repeat allocations for the common
  no-diagnostics path.
- Keep factory methods as the primary construction path:
  - `consumed(consumedTokens, availableTokens, diagnostics = RateLimitDiagnostics.EMPTY)`
  - `rejected(availableTokens, diagnostics = RateLimitDiagnostics.rejected(...))`
  - `error(cause)`

Validation:

- Update `RateLimitResultTest`.
- Add serialization round-trip and error redaction tests.
- Add KDoc/comment stating `RateLimitRejectionReason` is additive-only and enum
  names must remain stable; JVM enum serialization is sufficient.

### T1c. Public Error Redaction

- Sanitize public `errorMessage` values with a 256-character cap and URI
  `user:pass@host` credential redaction; do not fall back to raw
  `Throwable.toString()`. Header/query-token redaction is out of scope for this
  issue and can be added if a concrete caller passes those values as exception
  messages.
- Keep server-side logs unchanged except for existing debug/warn levels: the
  redaction rule applies to public `RateLimitResult`, while application logs are
  operator-controlled and already local to the service.

Validation:

- Redaction unit tests cover URI credentials, no-message exception, and cap
  length.

### T1d. Provider Key-Size Enforcement

- Add `MAX_BUCKET_KEY_BYTES` and a shared validation helper for serialized
  bucket keys.
- Enforce the cap in `AbstractLocalBucketProvider`, `BucketProxyProvider`, and
  `AsyncBucketProxyProvider` after prefix application.
- Keep default prefix behavior but document that production callers should pass
  application/policy-specific prefixes.

Validation:

- Add local and distributed provider unit tests that reject oversized keys.

### T2. Probe Diagnostic Mapping

- Update `toRateLimitResult(ConsumptionProbe, requestedTokens)` to preserve
  retry-after/reset timing.
- Normalize `diagnostics.nanosToWaitForRefill = 0` when `probe.isConsumed` even
  if the upstream probe exposes a refill timing.
- Keep boolean overload deterministic with zero diagnostics.
- Extend probe usage tests for rejected diagnostics.

Validation:

- `RateLimiterProbeUsageTest` checks rejected `nanosToWaitForRefill`,
  `nanosToWaitForReset`, and `retryAfter`.
- `RateLimiterProbeUsageTest` checks consumed probes set refill wait to `0` and
  preserve reset wait.
- `retryAfter` is `null` when refill nanos is zero or negative.

### T3. Coroutine Behavior Coverage

- Add or strengthen focused cancellation tests for `SuspendLocalBucket` waiting
  behavior.
- Confirm suspend rate limiters still rethrow `CancellationException`.
- Add a slow `CompletableFuture` distributed suspend test that cancels while
  awaiting and proves no `RateLimitResult.error` is returned.
- Add `DistributedSuspendRateLimiter` timeout behavior with a limiter-scoped
  default and a concrete per-call overload.
- Implement timeout as an optional `defaultTimeout: kotlin.time.Duration? =
  null` constructor parameter on `DistributedSuspendRateLimiter`. The existing
  `SuspendRateLimiter.consume(key, numToken)` interface method uses that default.
- Document this constructor change in `CHANGELOG.md`; Kotlin callers keep source
  compatibility through the default value, but Java constructor callers need to
  use the new constructor shape if they instantiate directly.
- Add concrete overload
  `consume(key: String, numToken: Long, timeout: kotlin.time.Duration?)`.
- Keep `LocalSuspendRateLimiter` constructor unchanged because local immediate
  consume does not await remote I/O.
- Update `SuspendRateLimiter` KDoc to clarify immediate-consume semantics and
  implementation-specific timeout behavior.

Validation:

- `SuspendedLocalBucketTest`.
- Existing local/distributed suspend rate limiter tests.

### T4. Configuration Replacement Coverage

- Add tests for bandwidth IDs in `bucketConfiguration`.
- Add a local Bucket4j `replaceConfiguration(..., PROPORTIONALLY)` test using
  matching IDs.
- Add Lettuce and Redisson async proxy configuration replacement tests with
  matching bandwidth IDs when Testcontainers are available.
- If a local Testcontainers blocker prevents either backend test, record the
  exact blocker in the PR body and `docs/lessons/`.
  Use `docs/lessons/2026-05-14-issue-434-bucket4j-facade.md` for the blocker
  note when needed.

Validation:

- `ConfigurationSupportTest`.

### T5. Documentation Refresh

- Update `infra/bucket4j/README.md`.
- Update `infra/bucket4j/README.ko.md`.
- Cover module boundary, result diagnostics, provider lifecycle/expiration,
  async distributed support decision, configuration replacement with IDs,
  burst plus sustained examples, coroutine behavior, and
  Bucket4j-vs-Resilience4j guidance.
- Document timeout/cancellation behavior and that an in-flight Redis operation
  may complete after coroutine cancellation.
- Document production key-prefix guidance and serialized bucket key size cap.
- Update `docs/infra-deprecated-inventory.md` to remove or mark the
  `RateLimitResult` item as completed.

Validation:

- Grep README symbol names against source.
- Keep public GitHub/CHANGELOG/KDoc language policy intact.
- Run stale deprecated constructor search.

### T6. Verification and Review

- Run `git diff --check`.
- Run stale deprecated-constructor search.
- Run targeted tests:
  `./gradlew :bluetape4k-bucket4j:test --no-configuration-cache`.
- Run `./gradlew :bluetape4k-bucket4j:compileKotlin --no-configuration-cache`
  before tests if API edits surface compile errors.
- Run `./gradlew :bluetape4k-bucket4j:detekt --no-configuration-cache` when the
  task exists; if the module does not expose a detekt task, record that gap.
- Run `./gradlew :bluetape4k-bucket4j:koverXmlReport --no-configuration-cache`
  or the nearest available Kover report task; if unavailable, record the gap.
- Inspect the Kover XML/report for the new `RateLimitDiagnostics` and redaction
  helper surfaces when the report is available.
- Grep repo callers for `RateLimitResult(`, `component`, and `copy(` in
  `infra/bucket4j`, examples, and workshops.
- Run targeted compile if tests fail before test execution.
- Run Claude advisor/review gates and integrate all P0/P1 findings.
- Capture a concise lesson under `docs/lessons/`.

## Risks

- Removing the deprecated constructor is a public API break. It is explicitly
  accepted by issue #434 and the deprecated inventory.
- Adding diagnostics changes `RateLimitResult.copy()` and data-class member
  surface. Keep the new diagnostic data in one nested value to minimize
  destructuring/copy churn and document the break in CHANGELOG.
- Adding `diagnostics` changes data-class `componentN()` arity. Existing
  `component1` through `component4` remain, but downstream destructuring using
  all components may need source changes; document this separately from
  constructor removal.
- Redis integration tests depend on Testcontainers. If local Docker is blocked,
  run non-container unit tests plus report the validation gap; GitHub CI must
  still prove the full module.
- Adding too broad an async wrapper would expand the public surface. Keep this
  issue limited to the existing `DistributedSuspendRateLimiter` path.

## Review Notes

### Claude Code Opus Advisor

Artifact: `.omx/artifacts/claude-issue-434-bucket4j-plan-review-20260514-102810.md`

| Priority | Finding | Decision | Follow-up |
| --- | --- | --- | --- |
| P0 | Provider key-size cap was in spec but not plan. | Accepted. | Added T1d implementation and tests. |
| P0 | Factory signature updates were unspecified. | Accepted. | Added T1b canonical factory signatures and diagnostics constant. |
| P0 | Distributed timeout contract was unresolved. | Accepted. | Added constructor default, concrete overload, interface KDoc, and local stability decision. |
| P0 | Consumed probe refill-wait normalization was unspecified. | Accepted. | Added T2 normalization and tests. |
| P0 | Key-size validation test missing. | Accepted. | Added T1d validation. |
| P1/P2 | Detekt/Kover/componentN/rollback/test blocker details needed. | Accepted. | Added T6 tasks, risk bullet, and blocker note path. |

### Claude Code Opus Advisor Re-review

Artifact: `.omx/artifacts/claude-issue-434-bucket4j-plan-rereview-20260514-103044.md`

Result: P0=0, P1=0. Accepted P2 clarifications for explicit `Serializable`,
zero/negative retry-after boundary, bounded URI credential redaction scope, Java
constructor CHANGELOG note, and Kover surface inspection.

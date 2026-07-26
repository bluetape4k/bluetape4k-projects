# Issue 494 Cancellation Contracts Design

Date: 2026-05-20 Issue: #494 Repository: bluetape4k-projects

## Context

Several coroutine-facing modules recently needed cancellation fixes:

- `bluetape4k-coroutines` primitives must clear stale waiters when a suspended caller is cancelled.
- `infra/micrometer` suspend `Result` wrappers must not turn
  `CancellationException` into `Result.failure`.
- `io/retrofit2` suspend adapters must cancel the underlying HTTP call when the coroutine is cancelled.

The current tests cover these behaviours locally, but each module repeats its own cancellation scaffolding. Issue #494 asks for reusable contract helpers and representative adoption across these categories.

## External API Evidence

Kotlin official docs checked on 2026-05-20:

- `suspendCancellableCoroutine` throws `CancellationException` when the current job is cancelled while suspended and provides a prompt cancellation guarantee.
- `invokeOnCancellation` handlers can run at any time, can run concurrently with callback code, must be fast/non-blocking/thread-safe, and must not throw.
- `CancellationException` is the normal cancellation signal for cancellable suspending functions.

Design consequence: helper APIs should verify cancellation is rethrown, resource cleanup callbacks run, and test code does not mask cancellation behind
`runCatching`.

## Goals

1. Add reusable JUnit/coroutine contract helpers in `:bluetape4k-junit5`.
2. Provide a `Result` helper that catches non-cancellation failures only.
3. Apply helpers to at least three representative modules:
    - `:bluetape4k-coroutines`
    - `:infra:micrometer`
    - `:io:retrofit2`
4. Document the cancellation checklist and the `runCatching` rule in
   `testing/junit5` README files.
5. Keep tests local and CI-friendly; no external services or Testcontainers.

## Non-Goals

- Do not introduce a new production module.
- Do not rewrite every existing cancellation test in this PR.
- Do not change runtime behaviour unless a representative test exposes a bug.
- Do not add new external dependencies.

## Design

### Helper Location

Add `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CancellationContracts.kt`.

Rationale:

- `:bluetape4k-junit5` is already the shared test utility artifact.
- The representative modules already depend on it for tests.
- Keeping helpers in test infrastructure avoids pulling test-only contracts into runtime modules.

### Proposed Helper APIs

```kotlin
suspend fun <T> assertCancellationPropagates(
    timeout: Duration = 5.seconds,
    operation: suspend CoroutineScope.() -> T,
)

suspend fun assertCancellationClearsWaiter(
    timeout: Duration = 5.seconds,
    awaiter: suspend () -> Unit,
    releaser: () -> Unit,
)

suspend fun <T> assertResourceCancelledOnCoroutineCancellation(
    timeout: Duration = 5.seconds,
    beforeCancel: suspend () -> Unit = { yield() },
    resourceCancelled: () -> Boolean,
    operation: suspend CoroutineScope.() -> T,
)

inline fun <T> resultOfNonCancellation(block: () -> T): Result<T>
suspend inline fun <T> runCatchingNonCancellation(
    crossinline block: suspend () -> T,
): Result<T>
```

The names are intentionally explicit. These are test contracts, not generic assertion primitives.

### Representative Adoption

- `ResumableTest`: replace local cancelled-waiter scaffolding with
  `assertCancellationClearsWaiter`.
- `ObservationCoroutinesSupportTest`: replace repeated cancellation propagation scaffolding with `assertCancellationPropagates`.
- `SuspendRetrofitCallSupportTest`: add/replace a suspend HTTP cancellation test using `assertResourceCancelledOnCoroutineCancellation` and `MockWebServer`.

### Documentation

Update `testing/junit5/README.md` and `README.ko.md`:

- Add cancellation contract helpers to the feature list.
- Add a short coroutine cancellation checklist.
- State that `runCatching` is not acceptable around suspend APIs unless
  `CancellationException` is rethrown first; use
  `runCatchingNonCancellation`/`resultOfNonCancellation` when returning
  `Result`.

## Compatibility

- New public test utility APIs require English KDoc.
- Existing tests may keep their Korean names/comments.
- Helper APIs rely on existing dependencies already available from
  `:bluetape4k-junit5`: kotlinx.coroutines, bluetape4k assertions, and Kotlin test/JUnit infrastructure.

## Acceptance Criteria Mapping

- Shared test helper or fixture exists:
  `CancellationContracts.kt`.
- At least three representative modules use it:
  coroutines, micrometer, retrofit2.
- Documentation states when `runCatching` is not acceptable:
  `testing/junit5` README pair.
- CI can run tests without external services:
  representative tests use local coroutine test scheduler and MockWebServer.

## Risks

- Cancellation tests can be flaky if they race on startup. Mitigation: helpers use `withTimeout`, `yield`, and caller-provided `beforeCancel` hooks where the resource must be known to have started.
- Public helper names may become hard to rename after release. Mitigation:
  choose explicit contract names now and keep scope limited to test utilities.

## Step 2-R Current-Session Review

Claude Code CLI advisor was intentionally skipped because the user explicitly disabled Claude usage for this session. Codex current-session review covered the developer, security, Ops/SRE, and caller perspectives.

### Findings

| Priority | Perspective | Finding                                                                                                                   | Decision                                                                                                                                            |
|----------|-------------|---------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| P2       | Developer   | `assertResourceCancelledOnCoroutineCancellation` can race unless the caller can prove the wrapped request/future started. | Accepted in design: helper includes a caller-provided `beforeCancel` hook and the Retrofit adoption waits for MockWebServer to receive the request. |
| P2       | Caller      | Plain `runCatching` misuse is easy to reintroduce unless docs mention the rule near helper examples.                      | Accepted in design: README checklist explicitly covers this and points to non-cancellation helpers.                                                 |
| P3       | Ops/SRE     | Helper assertion failures should be timeout-bounded to avoid hanging CI jobs.                                             | Accepted in design: helpers use bounded waits.                                                                                                      |

### Convergence

- P0: 0
- P1: 0
- Open user questions: none

# Issue 494 Cancellation Contracts Plan

Date: 2026-05-20
Issue: #494
Spec: `docs/superpowers/specs/2026-05-20-issue-494-cancellation-contracts-design.md`

## Execution Plan

### 1. Add Shared Helpers

File:

- `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CancellationContracts.kt`

Tasks:

- Add `assertCancellationPropagates`.
- Add `assertCancellationClearsWaiter`.
- Add `assertResourceCancelledOnCoroutineCancellation`.
- Add `resultOfNonCancellation`.
- Add `runCatchingNonCancellation`.
- Write English KDoc with contracts and examples.

### 2. Add Helper Self-Tests

File:

- `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/coroutines/CancellationContractsTest.kt`

Tasks:

- Prove cancellation helper catches swallowed cancellation.
- Prove non-cancellation result helpers return `Result.failure`.
- Prove cancellation result helpers rethrow `CancellationException`.
- Prove resource-cancel helper checks the supplied cancellation predicate.

### 3. Adopt in `:bluetape4k-coroutines`

File:

- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ResumableTest.kt`

Tasks:

- Replace local cancelled-waiter scaffolding with
  `assertCancellationClearsWaiter`.
- Keep the READY fast-path regression test because it verifies a separate state
  transition.

### 4. Adopt in `:bluetape4k-micrometer`

File:

- `infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/observation/coroutines/ObservationCoroutinesSupportTest.kt`

Tasks:

- Replace repeated launch/cancel/result scaffolding with
  `assertCancellationPropagates`.
- Keep observation registry cleanup assertions where they exist.

### 5. Adopt in `:bluetape4k-retrofit2`

File:

- `io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/SuspendRetrofitCallSupportTest.kt`

Tasks:

- Add a local MockWebServer delayed-response test that starts a real Retrofit
  call, cancels the coroutine, and verifies `Call.isCanceled`.
- Use `assertResourceCancelledOnCoroutineCancellation` with a `beforeCancel`
  hook that waits until the server receives the request.

### 6. Documentation

Files:

- `testing/junit5/README.md`
- `testing/junit5/README.ko.md`

Tasks:

- Add the cancellation helpers to key features.
- Add usage examples.
- Add checklist:
  - rethrow `CancellationException`,
  - clear waiters/continuations,
  - cancel underlying futures/HTTP calls,
  - never wrap suspend code in plain `runCatching` unless cancellation is
    rethrown first.

### 7. Validation

Run in order:

```bash
./gradlew :bluetape4k-junit5:compileKotlin :bluetape4k-junit5:test --console=plain --no-configuration-cache
./gradlew :bluetape4k-coroutines:test --tests '*ResumableTest' --console=plain --no-configuration-cache
./gradlew :bluetape4k-micrometer:test --tests '*ObservationCoroutinesSupportTest' --console=plain --no-configuration-cache
./gradlew :bluetape4k-retrofit2:test --tests '*SuspendRetrofitCallSupportTest' --console=plain --no-configuration-cache
git diff --check
```

Also run IDE diagnostics on touched Kotlin files when available.

### 8. Review and PR

- Run current-session Step 6-R review on the changed diff.
- Resolve P0/P1 findings.
- Add `docs/lessons/2026-05-20-issue-494-cancellation-contracts.md`.
- Commit with Lore protocol.
- Push and open PR linked to #494.
- Watch GitHub checks.

## Rollback Plan

If the shared helper API proves unstable during implementation:

- Keep `runCatchingNonCancellation` and the narrow propagation helper.
- Defer waiter/resource helper broadening to a follow-up issue.
- Preserve representative tests locally without weakening existing runtime
  coverage.

## Step 3-R Current-Session Review

Claude Code CLI advisor was intentionally skipped because the user explicitly
disabled Claude usage for this session. Codex current-session review covered the
implementer, test engineer, architect, and delivery perspectives.

### Findings

| Priority | Perspective | Finding | Decision |
|---|---|---|---|
| P2 | Test engineer | Helper self-tests must include a negative case so `assertCancellationPropagates` cannot pass when cancellation is swallowed. | Accepted: Step 2 explicitly includes swallowed-cancellation proof. |
| P2 | Architect | New helper APIs expose coroutine-test contracts from `:bluetape4k-junit5`; avoid tying signatures to `kotlinx.coroutines.test` types. | Accepted: helper APIs use `CoroutineScope`, `Duration`, and core coroutine types only. |
| P2 | Delivery | README docs should live in `testing/junit5` rather than each adopted module to avoid repeated checklist drift. | Accepted: documentation task is centralized in the helper module README pair. |
| P3 | Ops/SRE | MockWebServer request wait must be bounded so cancellation tests cannot hang. | Accepted: Retrofit task uses a bounded `takeRequest` hook. |

### Convergence

- P0: 0
- P1: 0
- Open user questions: none

## Step 6-R Current-Session Review

Claude Code CLI advisor was intentionally skipped because the user explicitly
disabled Claude usage for this session. A native `code-reviewer` subagent was
started for an additional cross-check, but it did not return within the bounded
review window and was shut down. The final gate is based on current-session
six-tier review plus targeted validation.

### Findings

| Tier | Scope | P0 | P1 | P2/P3 | Result |
|---|---|---:|---:|---|---|
| 1 Security | Test helpers, tests, README docs | 0 | 0 | None | No secrets, auth, input, or deserialization surface introduced. |
| 2 Ops/SRE | Cancellation cleanup and bounded waits | 0 | 0 | None | Helper waits are timeout-bounded; Retrofit request wait is bounded. |
| 3 Structural | `bluetape4k-junit5` public helper API | 0 | 0 | None | API stays in testing module and depends only on core coroutine/Duration types. |
| 4 Kotlin/Quality | Changed Kotlin files | 0 | 0 | P2 fixed | Converted helper failure path so cancellation-to-non-cancellation conversion fails as an assertion instead of escaping as an arbitrary exception. |
| 5 Tests/Types | Self-tests and representative module adoption | 0 | 0 | None | Added positive and negative propagation tests, waiter cleanup, and resource cancellation coverage. |
| 6 Perf/Stability | Final diff | 0 | 0 | None | No unbounded polling or sleeps; hardcoded waits are bounded test-only probes. |

### Validation Evidence

```bash
./gradlew :bluetape4k-junit5:compileKotlin :bluetape4k-junit5:test --console=plain --no-configuration-cache
# SUCCESS: 269 tests

./gradlew :bluetape4k-coroutines:test --tests '*ResumableTest' \
  :bluetape4k-micrometer:test --tests '*ObservationCoroutinesSupportTest' \
  :bluetape4k-retrofit2:test --tests '*SuspendRetrofitCallSupportTest' \
  --console=plain --no-configuration-cache
# SUCCESS: 3 + 9 + 6 tests

git diff --check
# clean
```

### Convergence

- P0: 0
- P1: 0
- Remaining risk: Gradle emits existing deprecation warnings in unrelated
  dependency/test modules; no warning was introduced in the new helper contract.

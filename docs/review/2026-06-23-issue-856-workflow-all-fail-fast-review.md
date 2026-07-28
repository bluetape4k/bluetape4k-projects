# Issue #856 Workflow ALL Fail-Fast 검토

## Scope

- Branch: `fix/workflow-all-fail-fast-856`
- Module: `:bluetape4k-workflow`
- Issue: `#856`
- Files: blocking and suspend parallel workflow implementations, ALL policy docs, README policy notes, and fail-fast regression tests.

## RED Evidence

Before production code changed, the new fail-fast regression tests failed as expected in an `origin/develop` RED worktree with only the tests applied:

```text
./gradlew :bluetape4k-workflow:test \
  --tests "io.bluetape4k.workflow.core.ParallelWorkFlowTest.ALL policy cancels remaining work when one branch throws" \
  --tests "io.bluetape4k.workflow.core.ParallelWorkFlowTest.ALL policy cancels remaining work when one branch returns Failure" \
  --tests "io.bluetape4k.workflow.core.ParallelWorkFlowTest.ALL policy cancels remaining work when one branch returns Aborted" \
  --tests "io.bluetape4k.workflow.core.ParallelWorkFlowTest.ALL policy cancels remaining work when one branch returns Cancelled" \
  --tests "io.bluetape4k.workflow.coroutines.SuspendParallelFlowTest.ALL policy cancels remaining suspend work when one branch throws" \
  --tests "io.bluetape4k.workflow.coroutines.SuspendParallelFlowTest.ALL policy cancels remaining suspend work when one branch returns Failure" \
  --tests "io.bluetape4k.workflow.coroutines.SuspendParallelFlowTest.ALL policy cancels remaining suspend work when one branch returns Aborted" \
  --tests "io.bluetape4k.workflow.coroutines.SuspendParallelFlowTest.ALL policy cancels remaining suspend work when one branch returns Cancelled"
```

Result: FAILED, 0 passing and 8 failing.

- Blocking ALL fail-fast tests failed for thrown exception and returned `Failure`/`Aborted`/`Cancelled`: expected sibling interrupt signal, but `slowInterrupted=false`.
- Suspend ALL fail-fast tests failed for thrown exception and returned `Failure`/`Aborted`/`Cancelled`: expected sibling cancellation signal, but `slowCancelled=false`.

## Review Fixes

| Finding | Resolution |
|---|---|
| ALL fail-fast only covered thrown exceptions. | `Failure`, `Aborted`, and `Cancelled` reports are converted to `WorkNotSuccessException` inside each child so structured concurrency cancels siblings. |
| Blocking tests needed workflow-owned sibling cancellation proof. | Tests now use one workflow-owned slow branch and assert the exact interrupt signal. |
| Suspend tests needed cancellation semantics proof. | Tests now assert sibling cancellation and add explicit child `CancellationException` propagation coverage. |
| Concurrency helper choice needed rationale. | Test comments explain why `StructuredTaskScopeTester` and `SuspendedJobTester` do not fit this exact sibling-signal assertion. |

## 발견 사항

| Severity | Count | Notes |
|---|---:|---|
| P0 | 0 | No production data-loss, API-breaking, or CI-blocking issue found in the repaired diff. |
| P1 | 0 | Returned non-success reports and thrown exceptions both trigger ALL fail-fast behavior. |
| P2 | 0 | No remaining non-blocking review finding after the targeted fixes. |

## 증거

| Check | Result | Evidence |
|---|---|---|
| Targeted workflow tests | PASS | `./gradlew :bluetape4k-workflow:test --tests "io.bluetape4k.workflow.core.ParallelWorkFlowTest" --tests "io.bluetape4k.workflow.coroutines.SuspendParallelFlowTest"` completed with 32 tests. |
| Module test/build | PASS | `./gradlew :bluetape4k-workflow:test :bluetape4k-workflow:build` completed with 206 tests. |
| Diff hygiene | PASS | `git diff --check` produced no output. |
| Policy docs | PASS | `README.md`, `README.ko.md`, and `ParallelPolicy` describe ALL as all-success fail-fast. |

## Verdict

Gate passes with P0=0 and P1=0.

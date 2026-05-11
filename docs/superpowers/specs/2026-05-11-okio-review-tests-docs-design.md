# io/okio Review, Tests, and Docs Design

## Scope

- Module: `io/okio`
- Work type: strict 6-Tier code review, edge test hardening, public KDoc and README update.
- Verification mode: local targeted compile/test plus GitHub CI.

## Findings

| Priority | Finding | Evidence | Required Action |
|---|---|---|---|
| P0 | None | No release-blocking crash/data-loss path found in current review scope. | N/A |
| P1 | `RealBufferedSuspendedSource` can loop forever if the delegate `SuspendedSource.read()` repeatedly returns `0L` while callers wait for more bytes. | `request`, `skip`, `select`, `readAll(SuspendedSink)`, and `indexOf` loops read repeatedly without the no-progress guard used by `SuspendedSource.readAll`, sink interop, compression, and Tink code. | Add a shared no-progress read guard and edge tests for each looping path. |
| P1 | `RealBufferedSuspendedSource.close()` can swallow coroutine cancellation. | `close()` catches `Exception` and logs it; `CancellationException` is an `Exception` and should be rethrown per bluetape4k coroutine rules. | Rethrow `CancellationException` before logging other exceptions and add a regression test. |
| P1 | `RealBufferedSuspendedSource.close()` can hide ordinary close failures. | External advisor flagged divergence from Okio close propagation; callers could miss `IOException` from the delegate close. | Rethrow non-cancellation close exceptions after logging. |
| P1 | `readUtf8LineStrict(limit)` exact-limit CRLF branch was under-tested. | External advisor flagged the branch as requiring `scanLength + 1L` before indexing `buffer[scanLength]` and CRLF stripping via `readUtf8Line(scanLength)`. | Fix branch and add CRLF exact-limit regression test. |
| P1 | Single-read APIs and drain helpers needed explicit no-progress coverage. | External advisor flagged `read(ByteArray)`, `read(Buffer)`, `readUtf8`, `readByteArray`, and `readByteString`. | Add guards/tests for single reads and explicit tests for drain helpers. |
| P2 | Coroutine pipe tests do not stress lock/condition behavior with the repository-approved coroutine stress helper. | `SuspendedPipeTest` covers basic/cancel/fold behavior but not repeated concurrent producer-consumer cycles. | Add `SuspendedJobTester` coverage. |
| P2 | README/KDoc do not document the no-progress protection contract for buffered suspended sources. | README coroutine section explains async I/O but not repeated 0-byte delegate reads. | Add concise English/Korean docs and KDoc contract. |
| P2 | README lacks practical Okio adoption guidance. | README lists features, but does not explain Okio strengths, recommended usage scenarios, or anti-patterns. | Add synchronized English/Korean guidance sections. |

## Acceptance Criteria

- P0/P1 gate closes with zero remaining P0/P1 findings.
- `RealBufferedSuspendedSource` throws `IOException` after repeated no-progress delegate reads on looping APIs instead of spinning forever.
- `RealBufferedSuspendedSource.close()` rethrows `CancellationException` and ordinary close `IOException`.
- `readUtf8LineStrict(limit)` handles exact-limit CRLF without leaving `\r` or `\n` behind.
- New tests cover request/skip/select/indexOf/readAll no-progress paths, close cancellation, and `SuspendedJobTester` pipe concurrency.
- README.md and README.ko.md stay synchronized for the new coroutine safety contract and Okio guidance.

## Step Checklist Completion Report

| Item | Status | Notes |
|---|---|---|
| Worktree created | Done | `.worktrees/okio-review-tests-docs` from `origin/develop` |
| Current repo evidence checked | Done | Source, tests, README, build file inspected |
| User intent clear | Done | Apply strict 6-Tier review and fix/test/docs for `io/okio` |
| Review-only boundary | N/A | User asked to work on the module, not review-only |

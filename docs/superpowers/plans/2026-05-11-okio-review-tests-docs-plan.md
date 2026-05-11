# io/okio Review, Tests, and Docs Plan

## Plan

1. Fix P1 no-progress behavior in `RealBufferedSuspendedSource`.
   - Add a shared guarded read helper.
   - Use it in `request`, `skip`, `select`, `readAll(SuspendedSink)`, and `indexOf` loops.
2. Fix P1 cancellation propagation in `RealBufferedSuspendedSource.close()`.
   - Rethrow `CancellationException`.
   - Continue logging non-cancellation close failures.
3. Add edge and coroutine stability tests.
   - No-progress tests for looping buffered source APIs.
   - Close cancellation propagation test.
   - `SuspendedJobTester` pipe producer-consumer stress test.
4. Update public docs.
   - Korean KDoc for buffered suspended source no-progress contract.
   - README.md and README.ko.md coroutine safety notes.
   - README.md and README.ko.md Okio strengths, recommended usage scenarios, and anti-patterns.
5. Run strict gate verification.
   - `git diff --check`
   - `./gradlew :bluetape4k-okio:compileTestKotlin --no-build-cache`
   - targeted tests where feasible.
   - GitHub CI after PR creation.

## 6-Tier Gate

| Tier | Gate | Result Before Patch | Exit Requirement |
|---|---|---|---|
| 1 | Public API contract | No signature changes required; KDoc missing no-progress contract. | Public contract documented without binary API breakage. |
| 2 | Correctness / edge cases | P1 no-progress loops found. | Repeated `0L` delegate reads throw `IOException`. |
| 3 | Coroutine / concurrency safety | P1 cancellation swallow and P2 missing stress test found. | Cancellation rethrows; `SuspendedJobTester` coverage added. |
| 4 | Resource lifecycle | Close path logs ordinary close failures. | Cancellation is not hidden; close remains idempotent. |
| 5 | Documentation / examples | README lacked the new safety contract and practical Okio guidance. | README.md and README.ko.md synchronized with safety notes, strengths, usage scenarios, and anti-patterns. |
| 6 | Verification / maintainability | Existing tests cover many happy paths, but not these gates. | Targeted compile/tests and CI status recorded. |

## P0/P1 Gate

| Priority | Finding | Status | Validation |
|---|---|---|---|
| P0 | None | Closed | No P0 found in current scope |
| P1 | `RealBufferedSuspendedSource` no-progress loops | Closed | `BufferedSuspendSourceTest` targeted tests passed |
| P1 | `RealBufferedSuspendedSource.close()` swallows cancellation | Closed | Cancellation regression test passed |
| P1 | `RealBufferedSuspendedSource.close()` swallowed ordinary close `IOException` | Closed | IOException regression test passed |
| P1 | `readUtf8LineStrict(limit)` exact-limit CRLF branch could index beyond requested bytes and return the line with `\r` | Closed | CRLF exact-limit regression test passed |
| P1 | Single-read APIs could pass repeated delegate `0L` reads through as `0` | Closed | ByteArray/Buffer no-progress tests passed |
| P1 | Drain helpers needed explicit no-progress verification | Closed | `readUtf8`, `readByteArray`, `readByteString` no-progress tests passed |

## Advisor Review

Claude Code Opus advisor is required by the bluetape4k design workflow when available.

Artifact: `.omx/artifacts/claude-okio-review-20260511.md`

| Priority | Finding | Decision | Follow-up |
|---|---|---|---|
| P1 | `close()` swallowed non-cancellation `IOException` | Accepted | Rethrow and add regression test |
| P1 | `readUtf8LineStrict(limit)` exact-limit CRLF branch could OOB and return wrong slice | Accepted | Use `request(scanLength + 1L)` and `readUtf8Line(scanLength)`; add test |
| P1 | Single-read APIs lacked no-progress contract | Accepted | Guard `read(ByteArray)` and `read(Buffer)`; add tests |
| P1 | Drain helpers needed explicit no-progress tests | Accepted | Add `readUtf8`, `readByteArray`, `readByteString` tests |
| P2 | No-progress reset path untested | Accepted | Add recovering delegate test |
| P2 | Pipe stress coverage thin | Partially accepted | Keep stable producer-consumer `SuspendedJobTester`; rejected a cancellation-wakes-reader stress variant after it proved scheduler-sensitive locally |
| P2 | Decimal/hex no-progress and zero-byte request coverage could be more explicit | Accepted | Added focused tests for decimal/hex no-progress and `request(0)` |

## Verification Evidence

| Command | Result |
|---|---|
| `./gradlew :bluetape4k-okio:compileTestKotlin --no-build-cache` | Passed |
| `./gradlew :bluetape4k-okio:test --tests "io.bluetape4k.okio.coroutines.BufferedSuspendSourceTest" --tests "io.bluetape4k.okio.coroutines.SuspendedPipeTest" --no-build-cache` | Passed, 75 tests |
| `./gradlew :bluetape4k-okio:test --no-build-cache` | Passed, 1420 tests, 14 pending |
| `git diff --check` | Passed |

# Issue 789 Review - CSV Cold Flow File Readers

## Scope

- `io/csv`: file-based suspending CSV/TSV reader overloads.
- Regression tests for cold `Flow` creation, recollection, and early termination.

## Findings

- P0/P1: 0 after fix.
- Independent code-reviewer verdict: APPROVE, P0/P1 = 0.
- The bug came from opening `FileInputStream` before returning the `Flow`.
- File overloads now open the stream inside `flow {}` and close it with `use`, so collection owns the file handle lifecycle.
- The file-backed flow is moved upstream with `flowOn(Dispatchers.IO)` because parsing uses blocking file I/O.
- InputStream overload behavior is unchanged; callers that own an existing stream still own its lifecycle.

## Concurrency Test Gate

This is a deterministic cold-Flow and resource-lifecycle bug, not a concurrency
stress bug. `SuspendedJobTester` does not fit because the required evidence is
collection timing, recollection after early termination, and stream closure.

## Verification

- RED: `./gradlew :bluetape4k-csv:test --tests "io.bluetape4k.csv.coroutines.SuspendRecordReaderSupportTest"` failed with 3 failures: `FileNotFoundException` at Flow creation and `Stream closed` on recollection.
- GREEN targeted: same command passed with `10 passing`.
- GREEN module: `./gradlew :bluetape4k-csv:test` passed with `275 passing`.
- Hygiene: `git diff --check`.

## Residual Risk

The file-backed suspending overloads now run upstream parsing and transform work
on `Dispatchers.IO`. This is intentional for blocking file reads. Existing
InputStream overloads keep their previous collector-context behavior.

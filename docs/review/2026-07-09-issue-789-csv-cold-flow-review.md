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
- `io-csv-diagram-02` now shows the file-backed Flow lifecycle: open per collection and close on stop.

## Concurrency Test Gate

This is a deterministic cold-Flow and resource-lifecycle bug, not a concurrency
stress bug. `SuspendedJobTester` does not fit because the required evidence is
collection timing, recollection after early termination, and stream closure.

## Verification

- RED: `./gradlew :bluetape4k-csv:test --tests "io.bluetape4k.csv.coroutines.SuspendRecordReaderSupportTest"` failed with 3 failures: `FileNotFoundException` at Flow creation and `Stream closed` on recollection.
- GREEN targeted: same command passed with `10 passing`.
- GREEN module: `./gradlew :bluetape4k-csv:test` passed with `275 passing`.
- Hygiene: `git diff --check`.
- Diagram XML: `xmllint --noout docs/images/readme-diagrams/io-csv-diagram-02.svg`.
- Diagram render: `/Users/debop/.local/bin/cairosvg docs/images/readme-diagrams/io-csv-diagram-02.svg -o docs/images/readme-diagrams/io-csv-diagram-02.png -s 2`, PNG `4480 x 3000`.
- Diagram audits: connector `PASS markers=3 connectors=7 intrusions=0 crossings=0`; geometry `geometry_failures=0`; endpoint `PASS`; mixed-corner `PASS files=1 paths=7 q_bends=0 failures=0`.
- Diagram visual inspection: full-size PNG shows the new lifecycle card inside the Flow lane with no text, connector, or footer overlap.

## Residual Risk

The file-backed suspending overloads now run upstream parsing and transform work
on `Dispatchers.IO`. This is intentional for blocking file reads. Existing
InputStream overloads keep their previous collector-context behavior.

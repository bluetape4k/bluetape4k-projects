# Issue 651 CSV Okio Segments

## Context

Issue #651 targeted lower allocation and better throughput for large CSV parsing.
The existing `CsvLexer` decodes through a `Reader` and appends characters into a
`StringBuilder` per field.

## Decision

Use an internal UTF-8 fast path backed by Okio for supported CSV settings:
single-byte ASCII delimiter and quote characters with doubled-quote escaping.
The public `CsvRecordReader` API remains unchanged and falls back to the
existing reader-based lexer for unsupported charsets or settings.

Okio `Buffer.UnsafeCursor` is used only for read-only structural-byte scanning
inside the buffered source. Field bytes are moved between Okio buffers and
decoded once at field completion.

## Outcome

The cursor-backed implementation preserved existing CSV behavior and improved
throughput for the benchmarked public reader path:

- small: 20,173.731 -> 45,417.110 ops/s
- medium: 296.944 -> 683.434 ops/s
- large: 17.312 -> 40.115 ops/s

## Verification

- `./gradlew :bluetape4k-csv:test`
- `./gradlew :bluetape4k-csv:testBenchmark`

## Future Guidance

When using Okio `UnsafeCursor`, lock behavior with fixture equivalence tests
before optimizing. A naive byte loop was correct but only modestly faster; a
read-only cursor scan gave the useful performance gain while keeping memory
bounded by draining scanned buffer segments.

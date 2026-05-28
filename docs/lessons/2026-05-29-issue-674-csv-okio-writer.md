# 2026-05-29 - Issue 674 CSV Okio writer fast path

## Context

Issue #674 followed the CSV reader Okio work by targeting large CSV export
pipelines. `FlowCsvWriter.writeFile` already accepted a `Flow<Iterable<*>>`, but
UTF-8 file output still went through `OutputStreamWriter` and many small
`Writer.write(...)` calls.

## Decision

Keep the public `FlowCsvWriter` contract unchanged and route UTF-8 file output
through an internal Okio `BufferedSink` writer. Keep non-UTF-8 encodings on the
existing writer fallback path.

The fast path preserves the existing writer semantics:

- `null` as an unquoted empty field.
- empty string as quoted `""`.
- doubled-quote escaping.
- `quoteAll`.
- CSV and TSV delimiters and line separators.

## Outcome

The writer benchmark compares the legacy Writer baseline against the public
Okio-backed `writeFile` path:

| Workload | Writer baseline | Okio writer | Speedup |
|---|---:|---:|---:|
| small | 11,741.418 ops/s | 16,355.656 ops/s | 1.39x |
| medium | 676.110 ops/s | 2,068.696 ops/s | 3.06x |
| large | 83.802 ops/s | 272.157 ops/s | 3.25x |

## Verification

- `./gradlew :bluetape4k-csv:test --tests 'io.bluetape4k.csv.v2.FlowCsvWriterTest'` - 21 passing.
- `./gradlew :bluetape4k-csv:test` - 271 passing.
- `./gradlew :bluetape4k-csv:testBenchmark` - writer benchmark above.

## Future Guard

For CSV writer performance work, benchmark the public `writeFile` path and keep
a baseline implementation in the benchmark so future agents can compare against
the previous `Writer` behavior without reintroducing a production fallback flag.

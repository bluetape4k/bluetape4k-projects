# Benchmark Reports

This directory stores benchmark reports that need to outlive a PR comment.
Use it when a performance issue changes a hot path, adds a benchmark module, or
publishes chart artifacts under `docs/images/readme-charts/`.

## Report Index

| Report | Scope | Raw artifacts | Chart artifact |
|---|---|---|---|
| [Protobuf Caller-Owned Buffer Benchmark](https://github.com/bluetape4k/bluetape4k-projects/issues/757) | `:bluetape4k-protobuf`, `:protobuf-codec-benchmark`, issue #757; final report pending Task 8 evidence | Pending | Not produced |
| [Same-Condition IO Compressor Benchmark](./2026-06-11-io-same-condition-compressor-benchmark.md) | `:bluetape4k-io`, issue #746 | [`raw/issue-746/`](./raw/issue-746/) | Not produced |
| [ID Generators Self-Improve Benchmark](./2026-06-11-idgenerators-self-improve-benchmark.md) | `:bluetape4k-idgenerators`, issues #738/#739 | [`raw/issue-738/`](./raw/issue-738/) | Not produced |
| [Ktor CIO vs Spring WebFlux Benchmark](./2026-05-29-web-framework-benchmark.md) | `:web-framework-benchmark`, issue #667 | [`raw/2026-05-29-web-framework-*.json`](./raw/) | Not produced |
| [io/http HTTP Client Benchmark](./2026-05-21-io-http-client-benchmark.md) | `:bluetape4k-http`, issues #589/#590/#587 | Not retained | Not produced |
| [FastFory Codec Benchmark](./2026-04-25-fory-fast-codec-benchmark.md) | `:bluetape4k-redisson`, `:bluetape4k-lettuce`, issue #113 | Not retained | [`fory-fast-codec-uplift-chart-01.png`](../images/readme-charts/fory-fast-codec-uplift-chart-01.png) |

## Standard Report Shape

New benchmark reports should use this compact shape:

````markdown
# <Subject> Benchmark - YYYY-MM-DD

## Scope

- Issue or PR: #123
- Module or benchmark target: `:module-name`
- Decision being informed: <contract, default, migration, or follow-up>

## Commands

```bash
./gradlew :module:compileBenchmarkKotlin --no-configuration-cache
./gradlew :module:<benchmarkTask> --no-configuration-cache
````

## Run Conditions

| Field | Value |
|---|---|
| Date | YYYY-MM-DD |
| OS / CPU / Memory | ... |
| JDK | ... |
| Library versions | ... |
| JMH shape | fork/warmup/measurement/thread details |
| External services | Docker/Testcontainers/service versions, if any |

## Raw Artifacts

- `docs/benchmarks/raw/YYYY-MM-DD-subject.json`
- If raw files are not retained, state `Not retained` and explain why.

## Results

| Workload | Baseline | Candidate | Delta | Unit |
|---|---:|---:|---:|---|
| `benchmarkName` | ... | ... | ... | ops/s |

## Chart Artifacts

- `docs/images/readme-charts/<chart-name>.png`
- `docs/images/readme-charts/<chart-name>.svg`
- If no chart is produced, state `Not produced` and why.

## Interpretation

- State what the numbers prove.
- State what they do not prove.
- Separate fast rejection/backpressure counts from successful throughput.

## Follow-Up

- #124: <next issue>
```

## Chart Expectations

- Put reusable benchmark charts in `docs/images/readme-charts/`.
- Keep tables as the numeric source of truth; charts support scanning, not
  precision.
- Commit both SVG and PNG when the README references a PNG chart.
- Validate SVG with `xmllint --noout` and visually inspect generated PNGs when
  chart labels are dense or values are close.
- Do not compare unlike units in one chart. Split throughput, latency, startup,
  allocation, and failure-count charts when needed.

## Raw Result Expectations

- Prefer committing compact JSON or CSV under `docs/benchmarks/raw/` when it is
  small enough to review.
- Do not commit large profiler dumps, flamegraphs, or noisy logs. Store a short
  report here and link to the issue comment or artifact location instead.
- For Testcontainers-backed benchmarks, note image versions and whether the run
  was sequential. Treat pass-after-retry as environment evidence, not a clean
  first-pass result.

## Module Benchmark Docs

Module-local `Benchmark.md` files may stay concise. When results are used for an
issue or public README chart, add or update a report in this directory and link
the module document back to that report.

## Serializer Allocation Evidence

- [ByteBuffer Serializer Allocation Benchmark - 2026-07-18](2026-07-18-bytebuffer-serializer-allocation.md) — two fresh JMH GC-profiler runs for issue #1039 with committed raw JSON and CSV evidence.

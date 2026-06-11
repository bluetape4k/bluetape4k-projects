# IO Compressor Self-Improve Baseline - 2026-06-12

## Scope

- Issue: #751
- Source baseline: PR #750 merged into `develop`
- Module: `:bluetape4k-io`
- Benchmark runner: Gradle `kotlinx-benchmark`
- Primary metric: geometric mean of `SameConditionCompressorBenchmark.compress` over `gzip`, `deflate`, `zstd`, and `lz4` for all benchmark payload kinds and sizes.
- Direction: higher is better
- Target: at least 30% throughput improvement

## Command

```bash
scripts/io-compressor-self-improve-benchmark.sh
```

The wrapper runs:

```bash
./gradlew :bluetape4k-io:testSelfImproveBenchmark \
  -PbenchmarkInclude='.*SameConditionCompressorBenchmark.compress.*' \
  --no-build-cache \
  --rerun-tasks \
  -q
```

This remains a `kotlinx-benchmark` Gradle task. It does not use direct generated JMH jar execution.

## Baseline

| Metric | Value |
|---|---:|
| Baseline throughput | 19,757.605809808367 ops/s |
| 30% target | 25,684.88755275088 ops/s |

The initial review draft used the `json/small/lz4` cell as the only primary metric. That was too narrow after the review scope expanded to GZip, Deflate, Zstd, and LZ4, so the summary was recomputed from the same baseline raw JSON with the aggregate gate above.

## Raw Artifacts

- `docs/benchmarks/raw/issue-751/baseline-self-improve.json`
- `docs/benchmarks/raw/issue-751/baseline-summary.json`
- `docs/benchmarks/raw/issue-751/sealed.sha256`

## Sealed Files

Candidate implementation must not edit the benchmark fixture, parser, baseline, or sealed hash file after the aggregate gate correction:

- `io/io/build.gradle.kts`
- `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/SameConditionCompressorBenchmark.kt`
- `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/SameConditionCompressionPayloads.kt`
- `scripts/io-compressor-self-improve-benchmark.sh`
- `scripts/parse-io-compressor-self-improve.py`
- `docs/benchmarks/raw/issue-751/baseline-self-improve.json`
- `docs/benchmarks/raw/issue-751/baseline-summary.json`

Validate with:

```bash
scripts/validate-io-compressor-self-improve-sealed.sh
```

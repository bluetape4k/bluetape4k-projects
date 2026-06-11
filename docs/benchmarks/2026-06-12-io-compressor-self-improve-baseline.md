# IO Compressor Self-Improve Baseline - 2026-06-12

## Scope

- Issue: #751
- Source baseline: PR #750 merged into `develop`
- Module: `:bluetape4k-io`
- Benchmark runner: Gradle `kotlinx-benchmark`
- Primary metric: `SameConditionCompressorBenchmark.compress`, `payloadKind=json`, `payloadSize=small`, `compressorName=lz4`
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
| Baseline throughput | 1,148,860.5708701136 ops/s |
| 30% target | 1,493,518.7421311477 ops/s |

## Raw Artifacts

- `docs/benchmarks/raw/issue-751/baseline-self-improve.json`
- `docs/benchmarks/raw/issue-751/baseline-summary.json`
- `docs/benchmarks/raw/issue-751/sealed.sha256`

## Sealed Files

Candidate implementation must not edit the benchmark fixture, parser, baseline, or sealed hash file:

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

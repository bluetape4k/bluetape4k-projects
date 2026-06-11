# IO Compressor Self-Improve Reviewed Candidate - 2026-06-12

## Scope

- Issue: #751
- Source baseline: PR #750 merged into `develop`
- Module: `:bluetape4k-io`
- Benchmark runner: Gradle `kotlinx-benchmark`
- Primary metric: geometric mean of `SameConditionCompressorBenchmark.compress` over `gzip`, `deflate`, `zstd`, and `lz4` for all benchmark payload kinds and sizes.
- Direction: higher is better

## Command

```bash
scripts/io-compressor-self-improve-benchmark.sh
```

The wrapper runs the Gradle `kotlinx-benchmark` task `:bluetape4k-io:testSelfImproveBenchmark`; it does not run a raw generated JMH jar.

## Result

| Metric | Value |
|---|---:|
| Baseline throughput | 19,757.605809808367 ops/s |
| 30% target | 25,684.88755275088 ops/s |
| Reviewed candidate throughput | 19,739.301242587328 ops/s |
| Improvement | -0.09264125834021463% |
| Gate result | FAIL |

## Implementation

This revision removes the earlier repeated-input LZ4 cache and the ThreadLocal scratch/hint state because that favored the benchmark shape rather than improving codec throughput.

Review also rejected default compression-level changes that traded compression ratio for throughput. The default compression behavior is preserved:

- `GZipCompressor` keeps `Deflater.DEFAULT_COMPRESSION` by default and allows explicit compression-level configuration.
- `DeflateCompressor` keeps `Deflater.DEFAULT_COMPRESSION` by default, allows explicit compression-level configuration, and uses the repo default buffer size for its stream buffer.
- `ZstdCompressor` keeps default level 3.
- `LZ4Compressor` has no ThreadLocal, memoization, or header-write micro-change.

The reviewed candidate does not meet the 30% throughput target. It should not be treated as the winning self-improve result for issue #751.

## Raw Artifacts

- `docs/benchmarks/raw/issue-751/baseline-self-improve.json`
- `docs/benchmarks/raw/issue-751/baseline-summary.json`
- `docs/benchmarks/raw/issue-751/final-self-improve.json`
- `docs/benchmarks/raw/issue-751/final-summary.json`
- `docs/benchmarks/raw/issue-751/sealed.sha256`

## Validation

- `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.compressor.CompressorsTest' --tests 'io.bluetape4k.io.compressor.CompressorEdgeCaseTest' --tests 'io.bluetape4k.io.compressor.CompressorNullableApiTest' --no-build-cache --rerun-tasks`
- `scripts/io-compressor-self-improve-benchmark.sh`
- `scripts/validate-io-compressor-self-improve-sealed.sh`
- `git diff --check`

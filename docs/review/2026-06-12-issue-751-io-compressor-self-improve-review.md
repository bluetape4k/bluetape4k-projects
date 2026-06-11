# Issue 751 IO Compressor Self-Improve Review

## Verdict

- Code-safety P0: 0
- Code-safety P1: 0
- Issue-closeout acceptance blocker: 1
- Recommendation: non-closing evidence PR only

## Findings

No P0/P1 code-safety findings after removing the benchmark-shaped LZ4 cache and default compression-level reductions.

The performance acceptance gate still fails, so this candidate must not close issue #751. PR #752 is only acceptable as a non-closing evidence/guardrail PR that records rejected approaches and preserves the fair benchmark harness artifacts.

## Evidence

- Benchmark target: at least 25,684.88755275088 ops/s.
- Reviewed candidate benchmark: 19,879.398326293536 ops/s.
- Improvement: 0.6164335783271246%.
- Codec-level movement: gzip +1.4207026474347018%, deflate +1.7592347352817894%, zstd -2.217463289266508%, lz4 +1.5579510974993438%.
- Benchmark metric: geometric mean for GZip, Deflate, Zstd, and LZ4 compression over all same-condition payload kinds and sizes.
- Benchmark command: `scripts/io-compressor-self-improve-benchmark.sh`.
- Benchmark task: Gradle `kotlinx-benchmark` task `:bluetape4k-io:testSelfImproveBenchmark`.
- Correctness tests: `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.compressor.CompressorsTest' --tests 'io.bluetape4k.io.compressor.CompressorEdgeCaseTest' --tests 'io.bluetape4k.io.compressor.CompressorNullableApiTest' --no-build-cache --rerun-tasks`.
- Concurrency evidence: `CompressorEdgeCaseTest` covers platform-thread concurrency with `MultithreadingTester` and virtual-thread structured concurrency with `StructuredTaskScopeTester`.
- Sealed validation: `scripts/validate-io-compressor-self-improve-sealed.sh`.
- Diff validation: `git diff --check`.

## Residual Risk

The accepted review changes preserve default compression-ratio behavior, but the remaining implementation changes are not enough to satisfy the 30% throughput target. Issue #751 must stay open for a separate closeout path focused on the exact ByteArray benchmark path or an explicitly approved scope revision.

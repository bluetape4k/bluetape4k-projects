# Same-Condition IO Compressor Benchmark - 2026-06-11

## Scope

- Issue or PR: #746
- Module or benchmark target: `:bluetape4k-io`, `testBenchmark`
- Decision being informed: provide a stable JVM-side compressor-only matrix that can be compared with `bluetape-go` and `bluetape-rs`.

## Commands

```bash
./gradlew :bluetape4k-io:tasks --all
./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.benchmark.SameConditionCompressionPayloadsTest' --no-build-cache --rerun-tasks
./gradlew :bluetape4k-io:testBenchmarkCompile --no-build-cache --rerun-tasks
./gradlew :bluetape4k-io:testBenchmarkJar --no-build-cache --rerun-tasks
java -jar io/io/build/benchmarks/test/jars/bluetape4k-io-test-jmh-1.11.0-JMH.jar \
  '.*SameConditionCompressorBenchmark.compress.*' \
  -p payloadKind=json -p payloadSize=small -p compressorName=lz4 \
  -wi 1 -i 1 -r 100ms -w 100ms -f 1 \
  -rf json -rff docs/benchmarks/raw/issue-746/jmh-smoke.json
```

The Gradle `testBenchmark` JavaExec task exists and remains the primary full-run entrypoint. For the focused smoke run above, direct JMH jar execution is used because `testBenchmark --args` treats the first argument as the kotlinx runner input file, not as JMH include/filter arguments.

## Run Conditions

| Field | Value |
|---|---|
| Date | 2026-06-11 |
| OS / CPU | macOS Darwin 25.5.0, arm64 Apple workstation |
| JDK | Oracle GraalVM 25.0.3 LTS for the focused JMH smoke |
| Gradle / Kotlin | Gradle 9.5.0, repository Kotlin line |
| JMH shape | Smoke: fork 1, warmup 1 x 100 ms, measurement 1 x 100 ms, throughput, 1 thread |
| Payload matrix | JSON/Text/Binary/Random x small 1 KiB, medium 64 KiB, large 512 KiB |
| Normalized compressors | GZip, Deflate, Zstd, LZ4, Snappy |
| JVM-only context | BZip2, excluded from the normalized cross-ecosystem table |
| External services | None |

## Raw Artifacts

- `docs/benchmarks/raw/issue-746/payload-matrix.csv`
- `docs/benchmarks/raw/issue-746/large-payload-baseline.csv`
- `docs/benchmarks/raw/issue-746/jmh-smoke.json`
- Linked source comment: https://github.com/bluetape4k/bluetape4k-projects/issues/746#issuecomment-4677660014

## Results

The committed smoke result proves that the generated JMH jar executes the new benchmark class and writes JSON output:

| Workload | Score | Unit | Notes |
|---|---:|---|---|
| `SameConditionCompressorBenchmark.compress`, `json/small/lz4` | 1,021,334.375 | ops/s | 100 ms smoke only; not a ranking run |

Large payload baseline copied from the issue comment:

| payload | compressor | go MB/s | go ratio | jvm MB/s | jvm ratio | go bytes | jvm bytes |
|---|---:|---:|---:|---:|---:|---:|---:|
| json | gzip | 314.60 | 0.09661 | 219.81 | 0.097603 | 50649 | 51172 |
| json | deflate | 317.01 | 0.09657 | 217.38 | 0.09758 | 50631 | 51160 |
| json | lz4 | 1943.72 | 0.1649 | 2815.12 | 0.168257 | 86430 | 88215 |
| json | snappy | 2608.37 | 0.1761 | 2112.08 | 0.176342 | 92325 | 92454 |
| json | zstd | 1000.77 | 0.03897 | 1723.05 | 0.03639 | 20432 | 19079 |
| text | gzip | 824.72 | 0.003605 | 321.42 | 0.003597 | 1890 | 1886 |
| text | deflate | 889.99 | 0.003571 | 311.92 | 0.003574 | 1872 | 1874 |
| text | lz4 | 5854.16 | 0.004173 | 23918.64 | 0.004114 | 2188 | 2157 |
| text | snappy | 7087.83 | 0.04837 | 10745.96 | 0.048199 | 25362 | 25270 |
| text | zstd | 1785.20 | 0.0002594 | 9235.03 | 0.000246 | 136 | 129 |
| binary | gzip | 777.23 | 0.007381 | 324.16 | 0.007326 | 3870 | 3841 |
| binary | deflate | 824.36 | 0.007347 | 296.65 | 0.007303 | 3852 | 3829 |
| binary | lz4 | 5690.33 | 0.006569 | 23382.62 | 0.006504 | 3444 | 3410 |
| binary | snappy | 5954.70 | 0.06941 | 9296.57 | 0.069229 | 36391 | 36296 |
| binary | zstd | 964.49 | 0.03681 | 2278.99 | 0.036795 | 19298 | 19291 |
| random | gzip | 139.84 | 1 | 79.66 | 1.00034 | 524471 | 524466 |
| random | deflate | 140.16 | 1 | 77.69 | 1.00032 | 524453 | 524454 |
| random | lz4 | 7405.99 | 1 | 13067.62 | 1.00393 | 524307 | 526349 |
| random | snappy | 4410.34 | 1 | 5091.86 | 1.00005 | 524362 | 524315 |
| random | zstd | 1664.67 | 1 | 7008.52 | 1.00005 | 524313 | 524313 |

## Chart Artifacts

- Not produced. The issue needs a stable benchmark matrix and raw markdown/CSV evidence first; charts can be added when cross-repository report publication happens.

## Interpretation

- The repository now has a deterministic JVM fixture generator for JSON/Text/Binary/Random payloads at small/medium/large sizes.
- The normalized cross-ecosystem table is limited to GZip, Deflate, Zstd, LZ4, and Snappy. BZip2 remains JVM-only context because it is not a common normalized family across all target ecosystems.
- The smoke JMH result is execution evidence only. It must not be treated as a production ranking because it uses one short 100 ms measurement.
- The linked issue comment preserves the earlier large-payload cross-ecosystem snapshot; this PR makes the JVM harness durable and repeatable.

## Follow-Up

- Re-run the full matrix across `bluetape-go`, `bluetape-rs`, and `bluetape4k-io` from the same committed fixture contract before publishing public rankings.
- If allocation or GC evidence is required, run the generated JMH jar with `-prof gc` and commit a compact JSON/CSV extract under `docs/benchmarks/raw/issue-746/`.

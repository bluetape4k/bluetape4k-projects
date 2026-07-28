# Same-Condition IO Compressor Benchmark - 2026-06-11

## 범위

- Issue 또는 PR: #746
- Module 또는 benchmark target: `:bluetape4k-io`, `testBenchmark`
- 정보를 제공하는 결정: `bluetape-go`, `bluetape-rs`와 비교 가능한 안정적인 JVM-side
  compressor-only matrix를 제공한다.

## 명령

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

Gradle `testBenchmark` JavaExec task가 있으며 primary full-run entrypoint로 유지된다. 위의
focused smoke run에서는 direct JMH jar execution을 사용한다. `testBenchmark --args`가 첫
argument를 JMH include/filter argument가 아니라 kotlinx runner input file로 다루기 때문이다.

## 실행 조건

| 항목 | 값 |
|---|---|
| Date | 2026-06-11 |
| OS / CPU | macOS Darwin 25.5.0, arm64 Apple workstation |
| JDK | Oracle GraalVM 25.0.3 LTS for the focused JMH smoke |
| Gradle / Kotlin | Gradle 9.5.0, repository Kotlin line |
| JMH shape | Smoke: fork 1, warmup 1 x 100 ms, measurement 1 x 100 ms, throughput, 1 thread |
| Payload matrix | JSON/Text/Binary/Random x small 1 KiB, medium 64 KiB, large 512 KiB |
| Normalized compressors | GZip, Deflate, Zstd, LZ4, Snappy |
| JVM-only context | BZip2, excluded from the normalized cross-ecosystem table |
| External services | 없음 |

## Raw Artifact

- `docs/benchmarks/raw/issue-746/payload-matrix.csv`
- `docs/benchmarks/raw/issue-746/large-payload-baseline.csv`
- `docs/benchmarks/raw/issue-746/jmh-smoke.json`
- 연결된 source comment: https://github.com/bluetape4k/bluetape4k-projects/issues/746#issuecomment-4677660014

## 결과

Committed smoke result는 생성된 JMH jar가 새 benchmark class를 실행하고 JSON output을
쓴다는 점을 증명한다.

| Workload | Score | Unit | Notes |
|---|---:|---|---|
| `SameConditionCompressorBenchmark.compress`, `json/small/lz4` | 1,021,334.375 | ops/s | 100 ms smoke only; not a ranking run |

Issue comment에서 복사한 large payload baseline:

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

## Chart Artifact

- 생성하지 않았다. 이 issue는 먼저 안정적인 benchmark matrix와 raw markdown/CSV evidence가
  필요하다. Chart는 cross-repository report publication 시점에 추가할 수 있다.

## 해석

- Repository는 이제 small/medium/large size의 JSON/Text/Binary/Random payload에 대해
  deterministic JVM fixture generator를 가진다.
- Normalized cross-ecosystem table은 GZip, Deflate, Zstd, LZ4, Snappy로 제한한다. BZip2는
  모든 target ecosystem에서 공통 normalized family가 아니므로 JVM-only context로 남긴다.
- Smoke JMH result는 execution evidence일 뿐이다. 짧은 100 ms measurement 하나를 사용하므로
  production ranking으로 다루면 안 된다.
- 연결된 issue comment는 이전 large-payload cross-ecosystem snapshot을 보존한다. 이 PR은 JVM
  harness를 durable하고 repeatable하게 만든다.

## 후속 작업

- Public ranking을 publish하기 전에 같은 committed fixture contract에서 `bluetape-go`,
  `bluetape-rs`, `bluetape4k-io` 전체 matrix를 다시 실행한다.
- Allocation 또는 GC evidence가 필요하면 생성된 JMH jar를 `-prof gc`와 함께 실행하고 compact
  JSON/CSV extract를 `docs/benchmarks/raw/issue-746/` 아래에 commit한다.

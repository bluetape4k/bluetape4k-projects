# ID Generators Self-Improve Benchmark - 2026-06-11

## Scope

- Issue or PR: #738, #739
- Module or benchmark target: `:bluetape4k-idgenerators`
- Decision being informed: keep Snowflake allocation fast path and ULID/KSUID/KsuidMillis random payload improvements before the third comparison with `bluetape-go-idgenerators`.

## Commands

```bash
./gradlew :bluetape4k-idgenerators:benchmarkBenchmarkCompile
./gradlew :bluetape4k-idgenerators:focusedBenchmark
/Library/Java/JavaVirtualMachines/graalvm-jdk-21/Contents/Home/bin/java \
  -jar utils/idgenerators/build/benchmarks/benchmark/jars/bluetape4k-idgenerators-benchmark-jmh-1.11.0-JMH.jar \
  '.*FocusedSingleThreadIdGeneratorBenchmark.snowflakeDefaultWithUniqueness' \
  -p batchSize=65536 -wi 1 -i 3 -r 1s -w 1s -f 1 -prof gc -prof stack
```

## Run Conditions

| Field | Value |
|---|---|
| Date | 2026-06-11 |
| OS / CPU | macOS 26.5.1, Apple M4 Pro |
| JDK | GraalVM JDK 21.0.11 for JMH runs |
| Gradle / Kotlin | Gradle 9.5.0, Kotlin 2.3.20 |
| JMH shape | fork 1, warmup 3, measurement 5, 1s iterations, throughput, batch size 65,536 |
| Clock condition | Real system clock; Snowflake uses 4,096 sequence values per millisecond, so large batches can be clock-ceiling bound. |
| External services | None |

## Raw Artifacts

- `docs/benchmarks/raw/issue-738/baseline-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/candidate-1-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/candidate-2-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/candidate-3-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/focused-comparison-candidate-3.{md,csv}`
- `docs/benchmarks/raw/issue-738/baseline-snowflake-profile.txt`
- `docs/benchmarks/raw/issue-738/candidate-3-snowflake-profile.txt`
- `docs/benchmarks/raw/issue-738/candidate-3-ksuidMillisDefault-repeat.txt`

## Results

| Workload | Baseline | Candidate 3 | Delta | Unit |
|---|---:|---:|---:|---|
| `single.snowflakeDefaultGenerateOnly` | 62.515 | 62.530 | +0.02% | ops/s |
| `single.snowflakeDefaultWithUniqueness` | 62.504 | 62.495 | -0.01% | ops/s |
| `single.ulidMonotonicString` | 577.327 | 580.638 | +0.57% | ops/s |
| `single.ulidMonotonicValueOnly` | 831.422 | 847.083 | +1.88% | ops/s |
| `single.ulidMonotonicWithUniqueness` | 360.095 | 365.002 | +1.36% | ops/s |
| `single.ksuidSecondsFixedInstantString` | 79.546 | 88.611 | +11.40% | ops/s |
| `single.ksuidSecondsWithUniqueness` | 71.890 | 74.321 | +3.38% | ops/s |
| `single.ksuidMillisFixedInstantString` | 94.588 | 98.104 | +3.72% | ops/s |
| `single.ksuidMillisWithUniqueness` | 82.114 | 82.655 | +0.66% | ops/s |
| `concurrent.ulidMonotonicString` | 38.331 | 45.414 | +18.48% | ops/s |
| `concurrent.ulidMonotonicWithUniqueness` | 35.542 | 45.857 | +29.02% | ops/s |
| `concurrent.ksuidMillisDefaultString` | 68.632 | 70.704 | +3.02% | ops/s |
| `concurrent.ksuidMillisWithUniqueness` | 53.496 | 56.906 | +6.37% | ops/s |
| `concurrent.ksuidSecondsDefaultString` | 60.633 | 62.924 | +3.78% | ops/s |
| `concurrent.ksuidSecondsWithUniqueness` | 52.908 | 53.445 | +1.01% | ops/s |

## Allocation Profile

| Workload | Baseline | Candidate 3 | Delta | Unit |
|---|---:|---:|---:|---|
| `single.snowflakeDefaultWithUniqueness:gc.alloc.rate.norm` | 14,579,592.553 | 6,970,033.101 | -52.19% | B/op |
| `single.snowflakeDefaultWithUniqueness:gc.alloc.rate` | 868.813 | 415.382 | -52.19% | MB/s |
| `single.snowflakeDefaultWithUniqueness:gc.count` | 6 | 3 | -50.00% | count |

## Chart Artifacts

- Not produced. The raw CSV and markdown table are the numeric source of truth for this issue; charting is deferred to the blog/site step.

## Interpretation

- Snowflake throughput is effectively flat because the benchmark generates 65,536 IDs per operation against a 4,096/ms sequence ceiling. The useful win is allocation: the `SnowflakeId` intermediate object is avoided on `nextId()` and `nextIds(size)`, cutting normalized allocation by about 52%.
- ULID avoids `ByteArray(10)` random entropy staging and writes random bits directly into the ULID value/string path. The strongest visible effect is in concurrent monotonic string workloads.
- KSUID and KsuidMillis keep timestamp writes explicit and randomize only the payload length. Candidate 1 was rejected because randomizing the full 20-byte buffer and overwriting timestamp regressed KSUID throughput.
- `single.ksuidMillisDefaultString` in the full candidate 3 run measured 91.332 ops/s (-7.67%), but a same-JVM targeted repeat measured 97.193 ops/s (-1.74% vs baseline). Because the code path did not change between candidate 2 and candidate 3 for KSUID, treat the full-run dip as benchmark noise and keep the raw repeat evidence.
- Correctness remains guarded by Snowflake, ULID, KSUID, and KsuidMillis unit/concurrency tests.

## Follow-Up

- Run the third cross-language comparison against `bluetape-go-idgenerators` using the candidate 3 Kotlin code and the current Go implementation.
- Reuse these raw artifacts in the `bluetape4k.github.io` article for the Kotlin improvement section.

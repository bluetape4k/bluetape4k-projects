# ID Generators Self-Improve Benchmark - 2026-06-11

## 범위

- Issue 또는 PR: #738, #739
- Module 또는 benchmark target: `:bluetape4k-idgenerators`
- 정보를 제공하는 결정: `bluetape-go-idgenerators`와의 세 번째 비교 전에 Snowflake allocation
  fast path와 ULID/KSUID/KsuidMillis random payload 개선을 유지할지 판단한다.

## 명령

```bash
./gradlew :bluetape4k-idgenerators:benchmarkBenchmarkCompile
./gradlew :bluetape4k-idgenerators:focusedBenchmark
/Library/Java/JavaVirtualMachines/graalvm-jdk-21/Contents/Home/bin/java \
  -jar utils/idgenerators/build/benchmarks/benchmark/jars/bluetape4k-idgenerators-benchmark-jmh-1.11.0-JMH.jar \
  '.*FocusedSingleThreadIdGeneratorBenchmark.snowflakeDefaultWithUniqueness' \
  -p batchSize=65536 -wi 1 -i 3 -r 1s -w 1s -f 1 -prof gc -prof stack
```

## 실행 조건

| 항목 | 값 |
|---|---|
| Date | 2026-06-11 |
| OS / CPU | macOS 26.5.1, Apple M4 Pro |
| JDK | GraalVM JDK 21.0.11 for JMH runs |
| Gradle / Kotlin | Gradle 9.5.0, Kotlin 2.3.20 |
| JMH shape | fork 1, warmup 3, measurement 5, 1s iterations, throughput, batch size 65,536 |
| Clock condition | 실제 system clock. Snowflake는 millisecond당 4,096 sequence value를 사용하므로 큰 batch는 clock ceiling에 묶일 수 있다. |
| External services | 없음 |

## Raw Artifact

- `docs/benchmarks/raw/issue-738/baseline-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/candidate-1-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/candidate-2-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/candidate-3-focused.{txt,json,csv}`
- `docs/benchmarks/raw/issue-738/focused-comparison-candidate-3.{md,csv}`
- `docs/benchmarks/raw/issue-738/baseline-snowflake-profile.txt`
- `docs/benchmarks/raw/issue-738/candidate-3-snowflake-profile.txt`
- `docs/benchmarks/raw/issue-738/candidate-3-ksuidMillisDefault-repeat.txt`

## 결과

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

## Chart Artifact

- 생성하지 않았다. Raw CSV와 markdown table이 이 issue의 numeric source of truth이며,
  charting은 blog/site step으로 미룬다.

## 해석

- Benchmark가 4,096/ms sequence ceiling을 가진 Snowflake에 대해 operation당 65,536개 ID를
  생성하므로 Snowflake throughput은 사실상 flat하다. 유의미한 이득은 allocation이다.
  `nextId()`와 `nextIds(size)`에서 `SnowflakeId` intermediate object를 피해 normalized
  allocation을 약 52% 줄인다.
- ULID는 `ByteArray(10)` random entropy staging을 피하고 random bit를 ULID value/string
  path에 직접 쓴다. 가장 강하게 보이는 효과는 concurrent monotonic string workload에 있다.
- KSUID와 KsuidMillis는 timestamp write를 명시적으로 유지하고 payload length만 randomize한다.
  Candidate 1은 전체 20-byte buffer를 randomize하고 timestamp를 overwrite해 KSUID throughput을
  떨어뜨렸으므로 기각했다.
- Full candidate 3 run의 `single.ksuidMillisDefaultString`은 91.332 ops/s(-7.67%)였지만,
  same-JVM targeted repeat는 97.193 ops/s(-1.74% vs baseline)였다. KSUID의 code path가
  candidate 2와 candidate 3 사이에서 바뀌지 않았으므로 full-run dip은 benchmark noise로
  다루고 raw repeat evidence를 유지한다.
- Correctness는 Snowflake, ULID, KSUID, KsuidMillis unit/concurrency test가 계속 보호한다.

## 후속 작업

- Candidate 3 Kotlin code와 현재 Go implementation을 사용해 `bluetape-go-idgenerators`와 세 번째
  cross-language comparison을 실행한다.
- Kotlin improvement section을 위해 `bluetape4k.github.io` article에서 이 raw artifact를 재사용한다.

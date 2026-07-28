# ByteBuffer Serializer Allocation Benchmark - 2026-07-18

## 범위

Issue [#1039](https://github.com/bluetape4k/bluetape4k-projects/issues/1039)는 기존
`ByteArray`, compatibility-default `ByteBuffer`, concrete optimized `ByteBuffer`
serializer path의 allocation behavior를 측정한다. Deterministic payload는 JDK, Kryo,
Fory, Jackson 2, Jackson 3, Fastjson2 JSONB, Avro reflect 전반에서 동등하다. Setup,
round-trip validation, buffer allocation, buffer reset은 timed method 밖에 있다.

## 명령

```bash
./gradlew :serializer-benchmark:clean :serializer-benchmark:benchmarkBenchmarkJar --no-configuration-cache
java -jar benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/*-JMH.jar \
  '.*SerializerAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff jmh.json
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run --input jmh.json --output summary.csv
```

## 실행 조건

- Commit: `57a2468e9e7cb2c305a7046dd8baa10f71284f21`
- Run ID: `run-20260718T030512Z`, `run-20260718T031704Z`
- JMH 1.37, Java 21.0.11 GraalVM, macOS; exact host details are in each `environment.txt`.
- JMH process는 한 번에 하나씩 실행했고 fork 2, warmup 3, measurement 5, thread 1을 사용했다.
- Primary metric: B/op 단위의 `gc.alloc.rate.norm`. Throughput은 diagnostic only이다.
- Reduction claim은 두 run 모두 같은 방향으로 5% 이상 개선되어야 한다.

## Raw Artifact

- [Run 1 environment](raw/issue-1039/run-20260718T030512Z/environment.txt), [JMH JSON](raw/issue-1039/run-20260718T030512Z/jmh.json), [summary CSV](raw/issue-1039/run-20260718T030512Z/summary.csv)
- [Run 2 environment](raw/issue-1039/run-20260718T031704Z/environment.txt), [JMH JSON](raw/issue-1039/run-20260718T031704Z/jmh.json), [summary CSV](raw/issue-1039/run-20260718T031704Z/summary.csv)
- [Two-run comparison CSV](raw/issue-1039/comparison.csv)
- Chart: 생성하지 않았다. Table과 committed raw file이 numeric source of truth다.

## Allocation 결과

각 값은 run별 `ByteArray baseline -> candidate B/op (delta)` 형식이다.

| Candidate | Run 1 | Run 2 | Verdict |
|---|---:|---:|---|
| `avroReflectDeserializeCompatibility` | 42713.8 -> 43511.9 (1.87%) | 42696.6 -> 43478.8 (1.83%) | ineligible |
| `avroReflectDeserializeOptimized` | 42713.8 -> 42695.9 (-0.04%) | 42696.6 -> 42701.7 (0.01%) | inconclusive |
| `avroReflectSerializeCompatibility` | 102824.5 -> 102800.5 (-0.02%) | 102817.3 -> 102808.5 (-0.01%) | ineligible |
| `avroReflectSerializeOptimized` | 102824.5 -> 100750.7 (-2.02%) | 102817.3 -> 100721.1 (-2.04%) | inconclusive |
| `fastjsonDeserializeFallbackDirect` | 2176.0 -> 3336.0 (53.31%) | 2176.0 -> 3336.0 (53.31%) | ineligible |
| `fastjsonDeserializeFallbackReadOnly` | 2176.0 -> 3336.0 (53.31%) | 2176.0 -> 3336.0 (53.31%) | ineligible |
| `fastjsonDeserializeOptimizedHeap` | 2176.0 -> 2176.0 (0.00%) | 2176.0 -> 2176.0 (0.00%) | inconclusive |
| `fastjsonSerializeFallback` | 1160.0 -> 1160.0 (0.00%) | 1160.0 -> 1160.0 (0.00%) | ineligible |
| `foryDeserializeOptimized` | 2576.0 -> 2640.0 (2.48%) | 2576.0 -> 2640.0 (2.48%) | inconclusive |
| `forySerializeFallback` | 1480.0 -> 1480.0 (0.00%) | 1480.0 -> 1480.0 (0.00%) | ineligible |
| `jackson2DeserializeCompatibility` | 4136.0 -> 5664.0 (36.94%) | 4136.0 -> 5664.0 (36.94%) | ineligible |
| `jackson2DeserializeOptimized` | 4136.0 -> 4216.0 (1.93%) | 4136.0 -> 4216.0 (1.93%) | inconclusive |
| `jackson2SerializeCompatibility` | 2032.0 -> 2032.0 (0.00%) | 2032.0 -> 2032.0 (0.00%) | ineligible |
| `jackson2SerializeOptimized` | 2032.0 -> 528.0 (-74.02%) | 2032.0 -> 528.0 (-74.02%) | accepted |
| `jackson3DeserializeCompatibility` | 4216.0 -> 5744.0 (36.24%) | 4216.0 -> 5760.0 (36.62%) | ineligible |
| `jackson3DeserializeOptimized` | 4216.0 -> 4296.0 (1.90%) | 4216.0 -> 4296.0 (1.90%) | inconclusive |
| `jackson3SerializeCompatibility` | 2048.0 -> 2048.0 (0.00%) | 2048.0 -> 2048.0 (0.00%) | ineligible |
| `jackson3SerializeOptimized` | 2048.0 -> 632.0 (-69.14%) | 2048.0 -> 632.0 (-69.14%) | accepted |
| `jdkDeserializeCompatibility` | 8992.0 -> 10376.0 (15.39%) | 8992.0 -> 10472.0 (16.46%) | ineligible |
| `jdkDeserializeOptimized` | 8992.0 -> 9040.0 (0.53%) | 8992.0 -> 8992.0 (0.00%) | inconclusive |
| `jdkSerializeCompatibility` | 12224.0 -> 12224.0 (0.00%) | 12224.0 -> 12224.0 (0.00%) | ineligible |
| `jdkSerializeOptimized` | 12224.0 -> 2648.0 (-78.34%) | 12224.0 -> 2648.0 (-78.34%) | accepted |
| `kryoDeserializeCompatibility` | 3328.0 -> 4580.0 (37.62%) | 3352.0 -> 4580.0 (36.63%) | ineligible |
| `kryoDeserializeOptimized` | 3328.0 -> 3120.0 (-6.25%) | 3352.0 -> 3108.0 (-7.28%) | accepted |
| `kryoSerializeCompatibility` | 1288.0 -> 1288.0 (0.00%) | 1288.0 -> 1288.0 (0.00%) | ineligible |
| `kryoSerializeOptimized` | 1288.0 -> 124.0 (-90.37%) | 1288.0 -> 112.0 (-91.30%) | accepted |

## Diagnostic Throughput

Throughput score와 error는 각 `summary.csv`와 raw JMH JSON에 보존되어 있다. 이는
regression을 진단하지만 allocation claim을 확립하지는 않는다.

## Claim 결정

- Accepted (5): Jackson 2 serialize, Jackson 3 serialize, JDK serialize, Kryo serialize,
  Kryo deserialize optimized cell.
- Inconclusive (7): Avro reflect serialize/deserialize, Fastjson2 array-backed deserialize,
  Fory deserialize, JDK/Jackson 2/Jackson 3 deserialize optimized cell.
- Ineligible (14): compatibility와 fallback control. 이는 ergonomic comparison일 뿐이다.

## Optimized And Fallback Matrix

| Backend | Output path | Input path |
|---|---|---|
| JDK | optimized, accepted | optimized, inconclusive |
| Kryo | optimized, accepted | optimized, accepted |
| Fory | fallback, ergonomic-only | optimized, inconclusive |
| Jackson 2 | optimized, accepted | optimized, inconclusive |
| Jackson 3 | optimized, accepted | optimized, inconclusive |
| Fastjson2 | fallback, ergonomic-only | array-backed optimized but inconclusive; direct/read-only fallback |
| Avro reflect | optimized but inconclusive | optimized but inconclusive |

## 한계

이 측정은 committed payload, default serializer configuration, reflect Avro implementation,
명명된 ByteBuffer path에만 적용된다. Wire format, security default, ownership, production
behavior를 바꾸지 않는다. Caller는 충분한 remaining capacity를 가진 writable target을 제공해야
한다. Output은 성공 후에만 caller position을 전진시키고, input은 duplicate를 통해 읽어 caller
state를 보존한다. Overflow와 read-only failure도 caller state를 보존한다.
Generic/specific/list Avro variant와 관련 없는 serializer variant는 측정하지 않았다.

## 후속 작업

Issue #755, #756, #757, #758은 deferred 상태로 남는다. 향후 dispatch, payload, serializer
change가 있으면 allocation-reduction wording을 재사용하기 전에 fresh run 2개를 만들어야 한다.

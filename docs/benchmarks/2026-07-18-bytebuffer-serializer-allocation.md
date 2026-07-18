# ByteBuffer Serializer Allocation Benchmark - 2026-07-18

## Scope

Issue [#1039](https://github.com/bluetape4k/bluetape4k-projects/issues/1039) measures the allocation behavior of the existing `ByteArray`, compatibility-default `ByteBuffer`, and concrete optimized `ByteBuffer` serializer paths. The deterministic payload is equivalent across JDK, Kryo, Fory, Jackson 2, Jackson 3, Fastjson2 JSONB, and Avro reflect. Setup, round-trip validation, buffer allocation, and buffer reset are outside timed methods.

## Commands

```bash
./gradlew :serializer-benchmark:clean :serializer-benchmark:benchmarkBenchmarkJar --no-configuration-cache
java -jar benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/*-JMH.jar \
  '.*SerializerAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff jmh.json
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run --input jmh.json --output summary.csv
```

## Run Conditions

- Commit: `57a2468e9e7cb2c305a7046dd8baa10f71284f21`
- Run IDs: `run-20260718T030512Z`, `run-20260718T031704Z`
- JMH 1.37, Java 21.0.11 GraalVM, macOS; exact host details are in each `environment.txt`.
- One JMH process ran at a time with two forks, three warmups, five measurements, and one thread.
- Primary metric: `gc.alloc.rate.norm` in B/op. Throughput is diagnostic only.
- A reduction claim requires both runs to improve by at least 5% in the same direction.

## Raw Artifacts

- [Run 1 environment](raw/issue-1039/run-20260718T030512Z/environment.txt), [JMH JSON](raw/issue-1039/run-20260718T030512Z/jmh.json), [summary CSV](raw/issue-1039/run-20260718T030512Z/summary.csv)
- [Run 2 environment](raw/issue-1039/run-20260718T031704Z/environment.txt), [JMH JSON](raw/issue-1039/run-20260718T031704Z/jmh.json), [summary CSV](raw/issue-1039/run-20260718T031704Z/summary.csv)
- [Two-run comparison CSV](raw/issue-1039/comparison.csv)
- Charts: Not produced. The tables and committed raw files are the numeric source of truth.

## Allocation Results

Values show `ByteArray baseline -> candidate B/op (delta)` for each run.

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

Throughput scores and errors are preserved in each `summary.csv` and raw JMH JSON. They diagnose regressions but do not establish allocation claims.

## Claim Decisions

- Accepted (5): Jackson 2 serialize, Jackson 3 serialize, JDK serialize, Kryo serialize, and Kryo deserialize optimized cells.
- Inconclusive (7): Avro reflect serialize/deserialize, Fastjson2 array-backed deserialize, Fory deserialize, and JDK/Jackson 2/Jackson 3 deserialize optimized cells.
- Ineligible (14): compatibility and fallback controls. These are ergonomic comparisons only.

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

## Limitations

The measurements apply only to the committed payload, default serializer configuration, reflect Avro implementation, and named ByteBuffer paths. They do not change wire formats, security defaults, ownership, or production behavior. Callers must provide a writable target with sufficient remaining capacity; output advances the caller's position only after success, while input is read through a duplicate and preserves caller state. Overflow and read-only failures preserve caller state. Generic/specific/list Avro variants and unrelated serializer variants are unmeasured.

## Follow-Up

Issues #755, #756, #757, and #758 remain deferred. Any future dispatch, payload, or serializer change must produce two fresh runs before allocation-reduction wording is reused.

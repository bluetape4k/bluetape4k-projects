# Serializer Allocation Benchmark

This non-published module proves allocation behavior for existing serializer `ByteArray`, compatibility `ByteBuffer`, and optimized `ByteBuffer` paths. It does not modify production dispatch, wire formats, ownership, or security configuration.

## Commands

```bash
./gradlew :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache
./gradlew :serializer-benchmark:benchmarkBenchmarkJar --no-configuration-cache
java -jar build/benchmarks/benchmark/jars/*-JMH.jar -l
java -jar build/benchmarks/benchmark/jars/*-JMH.jar '.*SerializerAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff jmh.json
```

Use `gc.alloc.rate.norm` (B/op) as the primary metric. Throughput is diagnostic. A positive claim requires two fresh runs that both improve by at least 5% in the same direction.

## Matrix

The 40 cells cover JDK (6), Kryo (6), Fory (4), Jackson 2 (6), Jackson 3 (6), Fastjson2 (6), and Avro reflect (6), with serialization and deserialization measured separately. Compatibility and fallback cells are ergonomic-only controls.

| Backend | Output | Input |
|---|---|---|
| JDK, Kryo | concrete optimized path | concrete optimized path |
| Fory | fallback | concrete optimized path |
| Jackson 2/3 | concrete optimized path | concrete optimized path |
| Fastjson2 | fallback | array-backed optimized; direct/read-only fallback |
| Avro reflect | concrete optimized path | concrete optimized path |

## Buffer Contract

Allocate caller-owned writable targets with enough remaining capacity. Successful output advances `position` by bytes written without widening `limit`; overflow/read-only failures roll back caller state. Input consumes a duplicate view and preserves the source `position` and `limit`.

Kotlin and Java callers use the same public `serializeTo`/`deserializeFrom` methods; the module benchmark fixtures show the Kotlin calls while the public module READMEs include both language forms.

See the [2026-07-18 report](../../docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md) and committed raw evidence. Issues #755, #756, #757, and #758 are explicitly out of scope.

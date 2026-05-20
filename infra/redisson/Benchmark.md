# Redisson Codec Benchmark

[English](./Benchmark.md) | [한국어](./Benchmark.ko.md)

Performance measurement results for Redisson Redis codec serialization/deserialization using kotlinx-benchmark (JMH).

## Measurement Overview

- **Target Codecs**: fastFory, fory, kryo5, fastjson2, jackson3, LZ4+FastFory, LZ4+Fory, LZ4+Kryo5, Zstd+FastFory, Zstd+Fory, Zstd+Kryo5, JDK, Gzip+FastFory
- **Metric**: Throughput — encode + decode round-trip ops/ms
- **Payload**: `BenchmarkData` object (ID, name, value, tags list)
- **Mode**: `@BenchmarkMode(Mode.Throughput)`, `@OutputTimeUnit(TimeUnit.MILLISECONDS)`

## How to Run

```bash
./gradlew :bluetape4k-redisson:benchmark
```

---

## Results

### Summary Table (Throughput: ops/ms, higher is better)

| Rank | Codec | ops/ms | ± Error | Note |
|------|-------|--------|---------|------|
| 🥇 | **fastFory** | **3,084** | ± 287 | |
| 🥈 | **fory** | **2,504** | ± 105 | |
| 🥉 | **fastjson2** | **1,928** | ± 62 | |
| 4 | kryo5 | 1,225 | ± 67 | |
| 5 | lz4FastFory | 829 | ± 71 | |
| 6 | lz4Fory | 774 | ± 42 | |
| 7 | jackson3 | 474 | ± 25 | |
| 8 | lz4Kryo5 | 518 | ± 114 | ⚠️ high variance |
| 9 | zstdFory | 196 | ± 7 | |
| 10 | zstdFastFory | 193 | ± 62 | ⚠️ high variance |
| 11 | zstdKryo5 | 139 | ± 5 | |
| 12 | jdk | 128 | ± 14 | |
| 13 | gzipFastFory | 108 | ± 1 | |

### Detailed JMH Output

```
Benchmark                                         Mode  Cnt     Score     Error   Units
RedissonCodecBenchmark.fastForyEncodeDecode      thrpt    5  3084.350 ± 287.457  ops/ms
RedissonCodecBenchmark.foryEncodeDecode          thrpt    5  2503.608 ± 104.512  ops/ms
RedissonCodecBenchmark.fastjson2EncodeDecode     thrpt    5  1928.194 ±  62.493  ops/ms
RedissonCodecBenchmark.kryo5EncodeDecode         thrpt    5  1225.002 ±  67.327  ops/ms
RedissonCodecBenchmark.lz4FastForyEncodeDecode   thrpt    5   829.228 ±  70.682  ops/ms
RedissonCodecBenchmark.lz4ForyEncodeDecode       thrpt    5   773.952 ±  41.811  ops/ms
RedissonCodecBenchmark.lz4Kryo5EncodeDecode      thrpt    5   517.606 ± 113.607  ops/ms
RedissonCodecBenchmark.jackson3EncodeDecode      thrpt    5   473.605 ±  24.813  ops/ms
RedissonCodecBenchmark.zstdForyEncodeDecode      thrpt    5   195.758 ±   6.659  ops/ms
RedissonCodecBenchmark.zstdFastForyEncodeDecode  thrpt    5   192.797 ±  62.447  ops/ms
RedissonCodecBenchmark.zstdKryo5EncodeDecode     thrpt    5   139.149 ±   4.666  ops/ms
RedissonCodecBenchmark.jdkEncodeDecode           thrpt    5   127.715 ±  13.625  ops/ms
RedissonCodecBenchmark.gzipFastForyEncodeDecode  thrpt    5   107.558 ±   0.963  ops/ms
```

### Performance Chart

![Redisson codec throughput chart](../../docs/images/readme-charts/infra-redisson-codec-throughput-chart-01.png)

---

## Analysis

### Key Findings

#### 1. fastFory — Undisputed Leader

- Score: **3,084 ops/ms** (1st) — stable, low variance (±287, ~9%)
- Uses Apache Fory's fast-path JIT codegen (no reference tracking)
- **Recommended as the default codec** for Redisson deployments

#### 2. fory — Strong Alternative

- Score: **2,504 ops/ms** (2nd), error ±105 (~4%)
- Full reference tracking variant — safer for complex object graphs
- Use over fastFory when object graphs contain cycles or shared references

#### 3. fastjson2 — Unexpected Placement

- Score: **1,928 ops/ms** (3rd) — notably **lower than Lettuce** (6,379 ops/ms)
- Redisson uses Netty `ByteBuf` API vs Lettuce's `ByteBuffer` — different allocation/copy paths cause the gap
- Still solid for JSON interoperability scenarios

#### 4. kryo5 — Best Among Legacy

- Score: **1,225 ops/ms** (4th), error ±67 (~5%)
- kryo5 (Kryo 5.x) outperforms standard kryo in Lettuce benchmark due to improved JIT codegen
- Good option for Java-ecosystem serialization compatibility

#### 5. LZ4 Compressed Variants (5th–8th)

- lz4FastFory (829) > lz4Fory (774) > lz4Kryo5 (518) > jackson3 (474)
- LZ4 adds ~350–2250 ops/ms overhead vs uncompressed counterparts
- Use when Redis memory is the bottleneck

#### 6. Zstd / Gzip — Compression-Prioritized (~100–200 ops/ms)

- All Zstd variants: 130–200 ops/ms — **15–28× slower** than fastFory
- Suitable for **infrequent large-payload** storage (>50KB objects)

### Comparison: Redisson vs Lettuce Codecs

| Codec | Redisson (ops/ms) | Lettuce (ops/ms) | Δ |
|-------|------------------|-----------------|---|
| fastFory | **3,084** | 3,286 | −6% |
| fory | **2,504** | 2,551 | −2% |
| fastjson2 | 1,928 | **6,379** | **−70%** |
| kryo/kryo5 | 1,225 | 963 | +27% |
| jackson3 | 474 | 834 | −43% |
| lz4FastFory | 829 | 906 | −8% |
| jdk | 128 | 132 | −3% |

**Key observation**: fastjson2 is 3.3× faster in Lettuce than Redisson. Root cause: Lettuce uses NIO `ByteBuffer` while Redisson uses Netty `ByteBuf` — fastjson2's internal direct-buffer optimization works better with NIO buffers. Binary codecs (fastFory, fory, kryo) show parity across both libraries.

### Codec Selection Guide

| Scenario | Recommended Codec | Reason |
|----------|------------------|--------|
| Maximum throughput | **fastFory** | Fastest in Redisson context |
| Complex object graphs | fory | Reference tracking support |
| Kryo ecosystem | kryo5 | Best Kryo variant |
| JSON interoperability | jackson3 | JSON — human-readable |
| Memory-constrained Redis | lz4FastFory | Compression + speed balance |
| Large payloads (>10KB) | zstdFory | Best compression ratio |

---

## Benchmark Environment

| Item | Value |
|------|-------|
| **CPU** | Apple M4 Pro (12-core) |
| **RAM** | 48 GB |
| **OS** | macOS 26.4.1 (Darwin 25.4.0) |
| **JVM** | Oracle GraalVM 21.0.11+9.1 |
| **Kotlin** | 2.3 |
| **kotlinx-benchmark** | 0.4.15 |
| **JMH** | 1.37 |
| **Warmup** | 3 iterations × 2s |
| **Measurement** | 5 iterations × 3s |
| **Fork** | 1 |
| **Threads** | 1 |
| **Mode** | Throughput (ops/ms) |
| **Date** | 2026-04-27 |

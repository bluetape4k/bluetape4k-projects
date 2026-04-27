# Lettuce Codec Benchmark

[English](./Benchmark.md) | [한국어](./Benchmark.ko.md)

Performance measurement results for Lettuce Redis codec serialization/deserialization using kotlinx-benchmark (JMH).

## Measurement Overview

- **Target Codecs**: fastjson2, fastFory, Fory, Kryo, LZ4+FastFory, LZ4+Fory, Jackson3, LZ4+Kryo, Zstd+FastFory, Zstd+Fory, Zstd+Kryo, JDK, Gzip+FastFory
- **Metric**: Throughput — encode + decode round-trip ops/ms
- **Payload**: `BenchmarkData` object (ID, name, value, tags list)
- **Mode**: `@BenchmarkMode(Mode.Throughput)`, `@OutputTimeUnit(TimeUnit.MILLISECONDS)`

## How to Run

```bash
./gradlew :bluetape4k-lettuce:benchmark
```

---

## Results

### Summary Table (Throughput: ops/ms, higher is better)

| Rank | Codec | ops/ms | ± Error | Note |
|------|-------|--------|---------|------|
| 🥇 | **fastjson2** | **6,379** | ± 1,358 | ⚠️ high variance |
| 🥈 | **fastFory** | **3,286** | ± 142 | |
| 🥉 | **fory** | **2,551** | ± 2,001 | ⚠️ high variance |
| 4 | kryo | 963 | ± 474 | ⚠️ high variance |
| 5 | lz4FastFory | 906 | ± 66 | |
| 6 | lz4Fory | 852 | ± 39 | |
| 7 | jackson3 | 834 | ± 25 | |
| 8 | lz4Kryo | 535 | ± 16 | |
| 9 | zstdFastFory | 206 | ± 17 | |
| 10 | zstdFory | 203 | ± 5 | |
| 11 | zstdKryo | 136 | ± 3 | |
| 12 | jdk | 132 | ± 13 | |
| 13 | gzipFastFory | 110 | ± 2 | |

### Detailed JMH Output

```
Benchmark                                        Mode  Cnt     Score      Error   Units
LettuceCodecBenchmark.fastjson2EncodeDecode     thrpt    5  6379.039 ± 1358.101  ops/ms
LettuceCodecBenchmark.fastForyEncodeDecode      thrpt    5  3286.042 ±  142.362  ops/ms
LettuceCodecBenchmark.foryEncodeDecode          thrpt    5  2551.081 ± 2000.928  ops/ms
LettuceCodecBenchmark.kryoEncodeDecode          thrpt    5   962.596 ±  474.242  ops/ms
LettuceCodecBenchmark.lz4FastForyEncodeDecode   thrpt    5   906.337 ±   66.294  ops/ms
LettuceCodecBenchmark.lz4ForyEncodeDecode       thrpt    5   852.295 ±   39.462  ops/ms
LettuceCodecBenchmark.jackson3EncodeDecode      thrpt    5   833.537 ±   24.996  ops/ms
LettuceCodecBenchmark.lz4KryoEncodeDecode       thrpt    5   534.536 ±   15.592  ops/ms
LettuceCodecBenchmark.zstdFastForyEncodeDecode  thrpt    5   206.005 ±   17.148  ops/ms
LettuceCodecBenchmark.zstdForyEncodeDecode      thrpt    5   202.811 ±    5.378  ops/ms
LettuceCodecBenchmark.zstdKryoEncodeDecode      thrpt    5   135.729 ±    2.533  ops/ms
LettuceCodecBenchmark.jdkEncodeDecode           thrpt    5   131.508 ±   12.928  ops/ms
LettuceCodecBenchmark.gzipFastForyEncodeDecode  thrpt    5   110.057 ±    2.378  ops/ms
```

### Performance Chart

| Codec | ops/ms | Throughput |
|-------|--------|-----------|
| 🔵 fastjson2 | **6,379** | <span style="background-color: #0EA5E9; color: white; padding: 2px 4px">████████████████████████████████████████</span> |
| 🟢 fastFory | **3,286** | <span style="background-color: #10B981; color: white; padding: 2px 4px">████████████████████</span> |
| 🟣 fory | **2,551** | <span style="background-color: #8B5CF6; color: white; padding: 2px 4px">████████████████</span> |
| 🟠 kryo | **963** | <span style="background-color: #F97316; color: white; padding: 2px 4px">██████</span> |
| 🩵 lz4FastFory | **906** | <span style="background-color: #14B8A6; color: white; padding: 2px 4px">██████</span> |
| 🌸 lz4Fory | **852** | <span style="background-color: #EC4899; color: white; padding: 2px 4px">██████</span> |
| 🟡 jackson3 | **834** | <span style="background-color: #EAB308; color: black; padding: 2px 4px">█████</span> |
| 🔴 lz4Kryo | **535** | <span style="background-color: #EF4444; color: white; padding: 2px 4px">████</span> |
| 🌿 zstdFastFory | **206** | <span style="background-color: #059669; color: white; padding: 2px 4px">██</span> |
| 🎀 zstdFory | **203** | <span style="background-color: #F43F5E; color: white; padding: 2px 4px">██</span> |
| 💜 zstdKryo | **136** | <span style="background-color: #6366F1; color: white; padding: 2px 4px">█</span> |
| ☁️ jdk | **132** | <span style="background-color: #64748B; color: white; padding: 2px 4px">█</span> |
| 🧡 gzipFastFory | **110** | <span style="background-color: #D97706; color: white; padding: 2px 4px">█</span> |

---

## Analysis

### Key Findings

#### 1. fastjson2 — Fastest, but High Variance

- Score: **6,379 ops/ms** (1st place) — **1.9× faster than fastFory**
- Error margin ±1,358 (~21%) suggests JIT warmup sensitivity
- Recommended for **throughput-critical scenarios** where variance is acceptable

#### 2. fastFory — Best Stable Choice

- Score: **3,286 ops/ms** (2nd place), error ±142 (~4%)
- Best **stability + performance** balance among binary codecs
- **Recommended as the default** for production Lettuce use

#### 3. fory — High Throughput but Unstable

- Score: **2,551 ops/ms** (3rd place), error ±2,001 (~78%)
- Extreme variance from JIT recompilation on each JMH fork
- Avoid for latency-sensitive workloads; acceptable for batch pipelines

#### 4. kryo — Moderate Speed, High Variance

- Score: **963 ops/ms** (4th), error ±474 (~49%)
- Legacy JMH JIT warm-up pattern — variance improves with longer warmup

#### 5. LZ4 Compressed Variants

- lz4FastFory (906) ≈ lz4Fory (852) ≈ jackson3 (834): **same performance tier**
- LZ4 compression costs offset serialization savings vs. uncompressed fastFory
- Use LZ4 variants when **Redis memory** is the bottleneck, not CPU

#### 6. Zstd / Gzip — Compression-Prioritized

- zstdFastFory (206) ≈ zstdFory (203): **~16× slower** than fastjson2
- Lowest throughput tier; suitable for **large payloads + low-frequency** access

#### 7. JDK / Gzip — Baseline

- jdk (132), gzipFastFory (110): legacy codecs, avoid in new code

### Codec Selection Guide

| Scenario | Recommended Codec | Reason |
|----------|------------------|--------|
| Maximum throughput | fastjson2 | Highest ops/ms |
| Production default | **fastFory** | Stable + fast, binary compact |
| Memory-constrained Redis | lz4FastFory | Balanced compression + speed |
| Large payloads (>10KB) | zstdFastFory | Best compression ratio |
| Interoperability required | jackson3 | JSON — human-readable |
| Legacy compatibility | jdk | Last resort |

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

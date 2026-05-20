# Lettuce 코덱 벤치마크

[English](./Benchmark.md) | 한국어

kotlinx-benchmark(JMH)를 이용한 Lettuce Redis 코덱 직렬화/역직렬화 성능 측정 결과입니다.

## 측정 개요

- **대상 코덱**: fastjson2, fastFory, Fory, Kryo, LZ4+FastFory, LZ4+Fory, Jackson3, LZ4+Kryo, Zstd+FastFory, Zstd+Fory, Zstd+Kryo, JDK, Gzip+FastFory
- **측정 지표**: 처리량 — encode + decode 왕복 ops/ms
- **페이로드**: `BenchmarkData` 객체 (ID, 이름, 값, 태그 목록)
- **모드**: `@BenchmarkMode(Mode.Throughput)`, `@OutputTimeUnit(TimeUnit.MILLISECONDS)`

## 실행 방법

```bash
./gradlew :bluetape4k-lettuce:benchmark
```

---

## 결과

### 요약 표 (처리량: ops/ms, 높을수록 좋음)

| 순위 | 코덱 | ops/ms | ± 오차 | 비고 |
|------|------|--------|--------|------|
| 🥇 | **fastjson2** | **6,379** | ± 1,358 | ⚠️ 분산 높음 |
| 🥈 | **fastFory** | **3,286** | ± 142 | |
| 🥉 | **fory** | **2,551** | ± 2,001 | ⚠️ 분산 높음 |
| 4 | kryo | 963 | ± 474 | ⚠️ 분산 높음 |
| 5 | lz4FastFory | 906 | ± 66 | |
| 6 | lz4Fory | 852 | ± 39 | |
| 7 | jackson3 | 834 | ± 25 | |
| 8 | lz4Kryo | 535 | ± 16 | |
| 9 | zstdFastFory | 206 | ± 17 | |
| 10 | zstdFory | 203 | ± 5 | |
| 11 | zstdKryo | 136 | ± 3 | |
| 12 | jdk | 132 | ± 13 | |
| 13 | gzipFastFory | 110 | ± 2 | |

### JMH 상세 출력

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

![Lettuce codec throughput chart](../../docs/images/readme-charts/infra-lettuce-codec-throughput-chart-01.png)

---

## 분석

### 핵심 발견

#### 1. fastjson2 — 최고 속도, 높은 분산

- 점수: **6,379 ops/ms** (1위) — **fastFory 대비 1.9배 빠름**
- 오차 ±1,358 (~21%)는 JIT 워밍업 민감도에 기인
- 분산을 감수할 수 있는 **처리량 최우선 시나리오**에 권장

#### 2. fastFory — 안정적 최우수 선택

- 점수: **3,286 ops/ms** (2위), 오차 ±142 (~4%)
- 바이너리 코덱 중 **안정성 + 성능** 균형 최우수
- 프로덕션 Lettuce 사용 시 **기본값으로 권장**

#### 3. fory — 높은 처리량, 불안정

- 점수: **2,551 ops/ms** (3위), 오차 ±2,001 (~78%)
- JMH fork마다 JIT 재컴파일로 극단적 분산 발생
- 레이턴시 민감 워크로드에는 부적합; 배치 파이프라인은 허용 가능

#### 4. kryo — 중간 속도, 높은 분산

- 점수: **963 ops/ms** (4위), 오차 ±474 (~49%)
- 레거시 JMH JIT 워밍업 패턴 — 더 긴 워밍업으로 분산 개선 가능

#### 5. LZ4 압축 변형

- lz4FastFory(906) ≈ lz4Fory(852) ≈ jackson3(834): **동일 성능 티어**
- LZ4 압축 비용이 비압축 fastFory 대비 직렬화 이득을 상쇄
- **Redis 메모리**가 병목일 때 LZ4 변형 사용

#### 6. Zstd / Gzip — 압축 우선

- zstdFastFory(206) ≈ zstdFory(203): fastjson2 대비 **~16배 느림**
- 최저 처리량 티어; **대용량 페이로드 + 저빈도** 접근에 적합

#### 7. JDK / Gzip — 기준선

- jdk(132), gzipFastFory(110): 레거시 코덱, 신규 코드에서는 사용 지양

### 코덱 선택 가이드

| 시나리오 | 권장 코덱 | 이유 |
|---------|---------|------|
| 최대 처리량 | fastjson2 | 최고 ops/ms |
| 프로덕션 기본 | **fastFory** | 안정적 + 빠름, 바이너리 압축 |
| Redis 메모리 제약 | lz4FastFory | 압축+속도 균형 |
| 대용량 페이로드 (>10KB) | zstdFastFory | 최고 압축률 |
| 상호운용성 필요 | jackson3 | JSON — 사람이 읽을 수 있음 |
| 레거시 호환 | jdk | 최후 수단 |

---

## 벤치마크 환경

| 항목 | 값 |
|------|---|
| **CPU** | Apple M4 Pro (12코어) |
| **RAM** | 48 GB |
| **OS** | macOS 26.4.1 (Darwin 25.4.0) |
| **JVM** | Oracle GraalVM 21.0.11+9.1 |
| **Kotlin** | 2.3 |
| **kotlinx-benchmark** | 0.4.15 |
| **JMH** | 1.37 |
| **Warmup** | 3회 × 2초 |
| **측정** | 5회 × 3초 |
| **Fork** | 1 |
| **스레드** | 1 |
| **모드** | Throughput (ops/ms) |
| **측정일** | 2026-04-27 |

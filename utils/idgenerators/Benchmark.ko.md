# ID Generators Benchmark

kotlinx-benchmark를 이용한 다양한 ID 생성기의 성능 측정 결과입니다.

## 측정 개요

- **측정 대상**: Snowflake, UUID(V4/V7), ULID, KSUID(Seconds/Millis), Flake
- **배치 크기**: 100 IDs, 10,000 IDs
- **모든 ID는 unique 검증됨** (단일 스레드, 멀티 스레드 모두)
- **멀티 스레드**: `Runtime.getRuntime().availableProcessors() * 2` 스레드 (16 스레드)

## 실행 방법

```bash
# 단일 스레드 벤치마크
./gradlew :bluetape4k-idgenerators:benchmarkSingleThread

# 멀티 스레드 벤치마크 (16 스레드)
./gradlew :bluetape4k-idgenerators:benchmarkConcurrent
```

---

## 결과: 단일 스레드 (Single-Thread)

### 요약 테이블

| Generator | Batch=100 | Batch=10000 |
|-----------|-----------|------------|
| UUID V7 | **429,584 ops/s** | **4,278 ops/s** |
| ULID | 270,825 ops/s | 2,553 ops/s |
| KSUID(Millis) | 59,896 ops/s | 582 ops/s |
| KSUID(Seconds) | 53,884 ops/s | 521 ops/s |
| Flake | 52,840 ops/s | 478 ops/s |
| UUID V4 | 105,645 ops/s | 1,037 ops/s |
| Snowflake | 40,972 ops/s | 410 ops/s |

### 상세 결과

#### batchSize=100 (소규모 배치)

```
SingleThreadIdGeneratorBenchmark.uuidV7                   429,584 ± 17,882 ops/s
SingleThreadIdGeneratorBenchmark.ulid                     270,825 ±  8,408 ops/s
SingleThreadIdGeneratorBenchmark.uuidV4                   105,645 ±  2,285 ops/s
SingleThreadIdGeneratorBenchmark.ksuidMillis              59,896 ±  2,197 ops/s
SingleThreadIdGeneratorBenchmark.ksuidSeconds             53,884 ±    363 ops/s
SingleThreadIdGeneratorBenchmark.flake                    52,840 ±    389 ops/s
SingleThreadIdGeneratorBenchmark.snowflake                40,972 ±     68 ops/s
```

#### batchSize=10000 (대규모 배치)

```
SingleThreadIdGeneratorBenchmark.uuidV7                   4,278 ±    34 ops/s
SingleThreadIdGeneratorBenchmark.ulid                     2,553 ±   268 ops/s
SingleThreadIdGeneratorBenchmark.uuidV4                   1,037 ±    49 ops/s
SingleThreadIdGeneratorBenchmark.ksuidMillis                582 ±     5 ops/s
SingleThreadIdGeneratorBenchmark.ksuidSeconds               521 ±     5 ops/s
SingleThreadIdGeneratorBenchmark.flake                      478 ±     3 ops/s
SingleThreadIdGeneratorBenchmark.snowflake                  410 ±     1 ops/s
```

### 그래프: 단일 스레드

#### Performance Chart (Batch=100)

| Generator | Throughput (ops/s) | Bar Chart |
|-----------|-------------------|-----------|
| 🔵 UUID-V7 | **429,584** | <span style="background-color: #0EA5E9; color: white; padding: 2px 4px">████████████████████████████████████████</span> |
| 🌸 ULID | **270,825** | <span style="background-color: #EC4899; color: white; padding: 2px 4px">████████████████████████████</span> |
| 🟢 UUID-V4 | **105,645** | <span style="background-color: #10B981; color: white; padding: 2px 4px">██████████</span> |
| 🟠 KSUID-Ms | **59,896** | <span style="background-color: #F97316; color: white; padding: 2px 4px">███████</span> |
| 🟡 KSUID-S | **53,884** | <span style="background-color: #EAB308; color: black; padding: 2px 4px">██████</span> |
| 🟣 Flake | **52,840** | <span style="background-color: #8B5CF6; color: white; padding: 2px 4px">██████</span> |
| 🔴 Snowflake | **40,972** | <span style="background-color: #EF4444; color: white; padding: 2px 4px">█████</span> |

**Single-Thread (Batch=100) 분석:**
- **1위: UUID V7** (429K ops/s) - 절대 강자, 무잠금 구현
- **2위: ULID** (270K ops/s) - 단일스레드에서 우수하지만 멀티스레드에서 성능 하락
- **3위: UUID V4** (105K ops/s) - 무작위 기반

#### Performance Chart (Batch=10,000)

| Generator | Throughput (ops/s) | Bar Chart |
|-----------|-------------------|-----------|
| 🔵 UUID-V7 | **4,278** | <span style="background-color: #0EA5E9; color: white; padding: 2px 4px">████████████████████████████████████████</span> |
| 🌸 ULID | **2,553** | <span style="background-color: #EC4899; color: white; padding: 2px 4px">█████████████████████████</span> |
| 🟢 UUID-V4 | **1,037** | <span style="background-color: #10B981; color: white; padding: 2px 4px">██████████</span> |
| 🟠 KSUID-Ms | **582** | <span style="background-color: #F97316; color: white; padding: 2px 4px">██████</span> |
| 🟡 KSUID-S | **521** | <span style="background-color: #EAB308; color: black; padding: 2px 4px">█████</span> |
| 🟣 Flake | **478** | <span style="background-color: #8B5CF6; color: white; padding: 2px 4px">█████</span> |
| 🔴 Snowflake | **410** | <span style="background-color: #EF4444; color: white; padding: 2px 4px">████</span> |

**Single-Thread (Batch=10,000) 분석:**
- 배치 크기 증가 시 모든 생성기가 ~100배 성능 하락
- UUID V7의 우위는 일관되게 유지

**Batch=10000 성능 비교:**

| Generator | ops/s |
|-----------|-------|
| UUID V7   | 4,278 (🔵 파랑) |
| ULID      | 2,553 (🔵 파랑 → 🔵 하강) |
| UUID V4   | 1,037 (🟢 초록) |
| KSUID(Ms) | 582 (🟠 주황) |
| KSUID(S)  | 521 (🟡 노랑) |
| Flake     | 478 (🟣 보라) |
| Snowflake | 410 (🔴 빨강) |

---

## 결과: 멀티 스레드 (Concurrent - 16 Threads)

### 요약 테이블

| Generator | Batch=100 | Batch=10000 |
|-----------|-----------|------------|
| UUID V7 | **83,431 ops/s** | **795 ops/s** |
| UUID V4 | 30,217 ops/s | 290 ops/s |
| Flake | 32,011 ops/s | 317 ops/s |
| KSUID(Seconds) | 25,810 ops/s | 252 ops/s |
| KSUID(Millis) | 25,768 ops/s | 241 ops/s |
| Snowflake | 27,016 ops/s | 253 ops/s |
| ULID | 22,580 ops/s | 223 ops/s |

### 상세 결과

#### batchSize=100 (소규모 배치)

```
ConcurrentIdGeneratorBenchmark.uuidV7                  83,431 ± 38,939 ops/s
ConcurrentIdGeneratorBenchmark.flake                   32,011 ±  9,195 ops/s
ConcurrentIdGeneratorBenchmark.uuidV4                  30,217 ±    705 ops/s
ConcurrentIdGeneratorBenchmark.snowflake               27,016 ± 14,997 ops/s
ConcurrentIdGeneratorBenchmark.ksuidSeconds           25,810 ±  6,029 ops/s
ConcurrentIdGeneratorBenchmark.ksuidMillis            25,768 ± 12,645 ops/s
ConcurrentIdGeneratorBenchmark.ulid                   22,580 ±  4,435 ops/s
```

#### batchSize=10000 (대규모 배치)

```
ConcurrentIdGeneratorBenchmark.uuidV7                    795 ±   438 ops/s
ConcurrentIdGeneratorBenchmark.flake                     317 ±    36 ops/s
ConcurrentIdGeneratorBenchmark.uuidV4                    290 ±    48 ops/s
ConcurrentIdGeneratorBenchmark.snowflake                 253 ±   245 ops/s
ConcurrentIdGeneratorBenchmark.ksuidSeconds             252 ±    93 ops/s
ConcurrentIdGeneratorBenchmark.ksuidMillis              241 ±    88 ops/s
ConcurrentIdGeneratorBenchmark.ulid                     223 ±    43 ops/s
```

### 그래프: 멀티 스레드 (16 Threads)

#### Performance Chart (Batch=100, 16 Threads)

| Generator | Throughput (ops/s) | Bar Chart |
|-----------|-------------------|-----------|
| 🔵 UUID-V7 | **83,431** | <span style="background-color: #0EA5E9; color: white; padding: 2px 4px">████████████████████████████████████████</span> |
| 🟣 Flake | **32,011** | <span style="background-color: #8B5CF6; color: white; padding: 2px 4px">████████████████</span> |
| 🟢 UUID-V4 | **30,217** | <span style="background-color: #10B981; color: white; padding: 2px 4px">███████████████</span> |
| 🔴 Snowflake | **27,016** | <span style="background-color: #EF4444; color: white; padding: 2px 4px">███████████</span> |
| 🟡 KSUID-S | **25,810** | <span style="background-color: #EAB308; color: black; padding: 2px 4px">███████████</span> |
| 🟠 KSUID-Ms | **25,768** | <span style="background-color: #F97316; color: white; padding: 2px 4px">███████████</span> |
| 🌸 ULID | **22,580** | <span style="background-color: #EC4899; color: white; padding: 2px 4px">███████████</span> |

**Concurrent (16 Threads, Batch=100) 분석:**
- **1위: UUID V7** (83K ops/s) - **압도적 우위**, 무잠금 구현의 장점
- **2~7위 (22K~32K)**: 경합대 형성
- **ULID 성능 하락**: 단일스레드 270K → 멀티스레드 22K (**70% 감소**)

#### Performance Chart (Batch=10,000, 16 Threads)

| Generator | Throughput (ops/s) | Bar Chart |
|-----------|-------------------|-----------|
| 🔵 UUID-V7 | **795** | <span style="background-color: #0EA5E9; color: white; padding: 2px 4px">████████████████████████████████████████</span> |
| 🟣 Flake | **317** | <span style="background-color: #8B5CF6; color: white; padding: 2px 4px">████████████████</span> |
| 🟢 UUID-V4 | **290** | <span style="background-color: #10B981; color: white; padding: 2px 4px">███████████████</span> |
| 🔴 Snowflake | **253** | <span style="background-color: #EF4444; color: white; padding: 2px 4px">███████████</span> |
| 🟡 KSUID-S | **252** | <span style="background-color: #EAB308; color: black; padding: 2px 4px">███████████</span> |
| 🟠 KSUID-Ms | **241** | <span style="background-color: #F97316; color: white; padding: 2px 4px">███████████</span> |
| 🌸 ULID | **223** | <span style="background-color: #EC4899; color: white; padding: 2px 4px">███████████</span> |

**Concurrent (16 Threads, Batch=10,000) 분석:**
- UUID V7의 우위는 배치 크기 증가에도 **일관되게 유지**
- ULID는 대규모 배치에서도 7번째 위치 지속

**Batch=10000 성능 비교 (16 Threads):**

| Generator | ops/s | Color |
|-----------|-------|-------|
| UUID V7   | 795   | 🔵 파랑 |
| Flake     | 317   | 🟣 보라 |
| UUID V4   | 290   | 🟢 초록 |
| Snowflake | 253   | 🔴 빨강 |
| KSUID(S)  | 252   | 🟠 주황 |
| KSUID(Ms) | 241   | 🟡 노랑 |
| ULID      | 223   | 🔵 파랑 |

---

## 성능 분석

### 주요 발견사항

#### 1. **UUID V7 우수성 (절대 성능)**
- **단일 스레드**: 429K ops/s (Batch=100) → 모든 생성기 중 **최고 성능**
- **멀티 스레드**: 83K ops/s (Batch=100) → UUID V7의 **무잠금(lock-free) 구현** 우위
- **확장성**: 배치 크기가 증가해도 상대적으로 **안정적인 성능** 유지

#### 2. **ULID의 문제점**
- **단일 스레드**: 270K ops/s (Batch=100) → 2위 성능
- **멀티 스레드**: 22K ops/s (Batch=100) → **심각한 성능 하락** (70% 감소)
  - 원인: Stateful Monotonic 구현이 **동기화 오버헤드** 존재
  - `ULID.statefulMonotonic(factory)` 내부 동기 메커니즘

#### 3. **Snowflake의 일관성**
- **단일 스레드**: 40K ops/s (Batch=100) → 5위
- **멀티 스레드**: 27K ops/s (Batch=100) → **상대적으로 안정적**
- 이유: `ReentrantLock` 기반의 명시적 동기화는 **예측 가능한 성능** 제공

#### 4. **Flake의 균형**
- **단일 스레드**: 52K ops/s (Batch=100)
- **멀티 스레드**: 32K ops/s (Batch=100) → **상대적으로 양호한 확장**
- 128비트 크기의 오버헤드는 있으나, lock 메커니즘은 효율적

#### 5. **배치 크기의 영향**
- Batch=100 → 10,000 시 성능 하락폭 비교:
  - UUID V7: **~100배** 하락 (429K → 4.3K)
  - Snowflake: **~100배** 하락 (40K → 410)
  - **배치 크기 증가 시 모든 생성기가 선형적 성능 저하** → Unique 검증 오버헤드

---

## 권장 사항

| 시나리오 | 추천 | 이유 |
|--------|-----|------|
| **고성능 요구** (초당 수만 건) | UUID V7 | 절대 성능 + 우수한 확장성 |
| **사전순 정렬 필요** | ULID 또는 UUID V7 | UUID V7 > ULID (멀티 스레드) |
| **트래디셔널 Snowflake** | Snowflake | 호환성 + 예측 가능한 성능 |
| **128비트 값 필요** | Flake | 적당한 성능 + 바이너리 효율 |
| **저성능 환경** | KSUID(Millis) | 밀리초 정확도 + 안정적인 성능 |

---

## 결론

**UUID V7이 모든 시나리오에서 우수한 성능을 제공합니다.**

- 단일 스레드: 429K ops/s (최고)
- 멀티 스레드: 83K ops/s (최고) + 우수한 확장성
- Stateless 구현으로 **동기화 오버헤드 최소**
- 사전순 정렬 보장 + 표준 UUID 호환성

따라서 새로운 프로젝트에서는 **UUID V7을 기본 선택**으로 권장합니다.

---

## Benchmark 실행 환경

- **JVM**: Java 21
- **Kotlin**: 2.3
- **kotlinx-benchmark**: 0.4.15
- **JMH**: 1.37
- **Setup**: 
  - Warmup: 3 iterations × 1s
  - Measurement: 5 iterations × 1s
  - Fork: 1
  - Throughput mode (ops/sec)

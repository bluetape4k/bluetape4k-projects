# LettuceNearCache 벤치마크

[English](./Benchmark.md) | [한국어](./Benchmark.ko.md)

kotlinx-benchmark(JMH)를 이용한 `LettuceNearCache` (L1=Caffeine, L2=Redis RESP3) 성능 측정 결과입니다.

## 아키텍처

```
LettuceNearCache
├── L1: Caffeine (프로세스 내, lock-free)
└── L2: Lettuce RESP3를 통한 Redis 7+
        └── CLIENT TRACKING → L1 자동 무효화
```

## 측정 시나리오

| 벤치마크 | 설명 | 측정 대상 |
|---------|------|---------|
| `l1Hit` | L1(Caffeine) 캐시 적중 | 순수 메모리 읽기, Redis 왕복 없음 |
| `l2Hit` | `clearLocal()` 후 L2(Redis) 적중 | `clearLocal()` 비용 + Redis 왕복 + L1 재충전 |
| `l2Miss` | 양쪽 모두 미스 | Redis GET이 null 반환 |
| `putSingle` | Write-through PUT (1건) | L1 + L2 쓰기 + CLIENT TRACKING GET |
| `putAll` | 배치 PUT (100건) | 100× L1 + L2 쓰기 |
| `removeSingle` | 1건 삭제 | L1 + L2 DEL (`@Setup(Level.Invocation)` pre-put 제외) |

> **참고**: `l2Hit` 측정값에는 `clearLocal()` 비용이 포함됩니다. 분석 섹션 참조.

## 실행 방법

```bash
./gradlew :bluetape4k-cache-lettuce:benchmark
```

Docker 필요 (Testcontainers Redis 7+).

---

## 결과

### 요약 표 (처리량: ops/ms, 높을수록 좋음)

| 벤치마크 | payloadSize=512 | payloadSize=4096 | payloadSize=16384 |
|---------|:--------------:|:----------------:|:-----------------:|
| **l1Hit** | **65,560 ± 10,861** | **63,458 ± 23,120** | **64,580 ± 9,507** |
| l2Hit | 4.067 ± 0.532 | 4.130 ± 0.452 | 3.930 ± 1.370 |
| l2Miss | 3.961 ± 1.394 | 3.917 ± 0.784 | 4.208 ± 0.408 |
| putSingle | 2.119 ± 0.100 | 2.077 ± 0.276 | 2.014 ± 0.152 |
| putAll (×100) | 1.038 ± 0.247 | 0.930 ± 0.118 | 0.407 ± 0.281 |
| removeSingle | 4.213 ± 0.177 | 4.243 ± 0.352 | 4.164 ± 0.271 |

### JMH 상세 출력

```
Benchmark                              (batchSize)  (payloadSize)   Mode  Cnt      Score       Error   Units
NearCacheBenchmark.l1Hit                       100            512  thrpt    5  65560.418 ± 10860.848  ops/ms
NearCacheBenchmark.l1Hit                       100           4096  thrpt    5  63457.547 ± 23120.211  ops/ms
NearCacheBenchmark.l1Hit                       100          16384  thrpt    5  64579.546 ±  9507.354  ops/ms
NearCacheBenchmark.l2Hit                       100            512  thrpt    5      4.067 ±     0.532  ops/ms
NearCacheBenchmark.l2Hit                       100           4096  thrpt    5      4.130 ±     0.452  ops/ms
NearCacheBenchmark.l2Hit                       100          16384  thrpt    5      3.930 ±     1.370  ops/ms
NearCacheBenchmark.l2Miss                      100            512  thrpt    5      3.961 ±     1.394  ops/ms
NearCacheBenchmark.l2Miss                      100           4096  thrpt    5      3.917 ±     0.784  ops/ms
NearCacheBenchmark.l2Miss                      100          16384  thrpt    5      4.208 ±     0.408  ops/ms
NearCacheBenchmark.putAll                      100            512  thrpt    5      1.038 ±     0.247  ops/ms
NearCacheBenchmark.putAll                      100           4096  thrpt    5      0.930 ±     0.118  ops/ms
NearCacheBenchmark.putAll                      100          16384  thrpt    5      0.407 ±     0.281  ops/ms
NearCacheBenchmark.putSingle                   100            512  thrpt    5      2.119 ±     0.100  ops/ms
NearCacheBenchmark.putSingle                   100           4096  thrpt    5      2.077 ±     0.276  ops/ms
NearCacheBenchmark.putSingle                   100          16384  thrpt    5      2.014 ±     0.152  ops/ms
NearCacheRemoveBenchmark.removeSingle          N/A            512  thrpt    5      4.213 ±     0.177  ops/ms
NearCacheRemoveBenchmark.removeSingle          N/A           4096  thrpt    5      4.243 ±     0.352  ops/ms
NearCacheRemoveBenchmark.removeSingle          N/A          16384  thrpt    5      4.164 ±     0.271  ops/ms
```

### 성능 차트: L1 적중 vs L2 연산

| 연산 | ops/ms (512B) | 처리량 |
|-----|:------------:|-------|
| ⚡ l1Hit | **65,560** | <span style="background-color: #0EA5E9; color: white; padding: 2px 4px">████████████████████████████████████████</span> |
| 🔄 removeSingle | 4.2 | <span style="background-color: #10B981; color: white; padding: 2px 4px">░</span> |
| 🔄 l2Hit | 4.1 | <span style="background-color: #8B5CF6; color: white; padding: 2px 4px">░</span> |
| 🔍 l2Miss | 4.0 | <span style="background-color: #F97316; color: white; padding: 2px 4px">░</span> |
| ✍️ putSingle | 2.1 | <span style="background-color: #EF4444; color: white; padding: 2px 4px">░</span> |
| 📦 putAll×100 | 1.0 | <span style="background-color: #EAB308; color: black; padding: 2px 4px">░</span> |

> L1 적중(65,560 ops/ms)과 L2 연산(~4 ops/ms) 사이 **16,000배 격차**.

---

## 분석

### 1. L1 적중 — 극한의 속도, 페이로드 무관

- **65,000–64,580 ops/ms** — 모든 페이로드 크기에서 동일 (512B → 16KB)
- Caffeine의 lock-free 읽기 경로는 페이로드 크기에 거의 영향받지 않음
- NearCache의 이론적 최대치 — 핫 경로 읽기는 반드시 L1에서 서비스해야 함

### 2. L2 적중 — `clearLocal()` 비용 포함

- **~4.0 ops/ms** — 벤치마크 본문에서 `clearLocal()` 호출로 인해 크게 제한됨
- **순수 L2 적중 레이턴시**: 키별 무효화나 사전 워밍된 L2 전용 키를 사용하면 더 높은 처리량 측정 가능
- localhost Redis RTT (Testcontainers): ~0.2–0.5ms → 이론적 최대 ~2,000–5,000 ops/ms
- 이 측정은 최악 케이스: "L1이 무효화된 상태에서 Redis 재조회"

### 3. L2 미스 — 일관된 Redis RTT

- **3.9–4.2 ops/ms** — 모든 페이로드에서 일관
- 페이로드 크기가 L2 미스에 영향 없음 (데이터 없이 키만 없음)
- l2Hit와 유사 → Redis RTT가 지배적

### 4. putSingle — Write-Through 오버헤드

- **~2.1 ops/ms** — l2Hit/Miss보다 현저히 느림
- 추가 오버헤드: `SET` 후 CLIENT TRACKING 등록을 위한 `GET` 추가 왕복
- `put` 호출당 Redis 왕복 2회 (SET + GET 트래킹)

### 5. putAll — 배치 쓰기, 페이로드 민감

- **512B: 1.038 ops/ms → 16KB: 0.407 ops/ms** — 페이로드 32배 증가에 **2.5배 성능 저하**
- 배치당 100건, 각각 CLIENT TRACKING GET → 호출당 200 Redis 연산
- 대용량 페이로드가 Redis 쓰기 대역폭 포화

### 6. removeSingle — 깔끔한 L1+L2 삭제

- **~4.2 ops/ms** — Redis `UNLINK` 1회 + L1 제거
- l2Hit/l2Miss와 거의 동일 → Redis RTT 지배

### 핵심 인사이트

| 인사이트 | 시사점 |
|--------|-------|
| L1 적중이 L2 대비 16,000배 빠름 | 적절한 `maxLocalSize`로 L1 적중률 최대화 |
| putSingle이 get의 2배 느림 | 쓰기 집중 워크로드는 CLIENT TRACKING 비용 지불 |
| putAll은 16KB에서 2.5배 저하 | 소용량 페이로드 사용 또는 대형 배치 분할 |
| 모든 L2 연산 ~4 ops/ms | Redis RTT(~250µs)가 공통 병목 |
| L1/읽기는 페이로드 크기 무관 | 캐시 값 역직렬화 비용 무시 가능 |

---

## 벤치마크 환경

| 항목 | 값 |
|------|---|
| **CPU** | Apple M4 Pro (12코어) |
| **RAM** | 48 GB |
| **OS** | macOS 26.4.1 (Darwin 25.4.0) |
| **JVM** | Oracle GraalVM 21.0.11+9.1 |
| **Redis** | 7+ (Testcontainers, Docker) |
| **Kotlin** | 2.3 |
| **kotlinx-benchmark** | 0.4.15 |
| **JMH** | 1.37 |
| **Warmup** | 3회 × 2초 |
| **측정** | 5회 × 3초 |
| **Fork** | 1 |
| **스레드** | 1 |
| **모드** | Throughput (ops/ms) |
| **batchSize** | 100 (putAll 전용) |
| **payloadSizes** | 512B, 4096B, 16384B |
| **측정일** | 2026-04-27 |

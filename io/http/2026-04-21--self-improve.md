# io/http 벤치마크 개선 세션 — 2026-04-21

## 목표

- 동기(Virtual Threads) / 비동기 / 코루틴 방식에서 HTTP 성능 최적화 설정 탐색
- 여러 HTTP 클라이언트 라이브러리 간 처리량 비교
- 캐시(인메모리 / 디스크) 및 gzip 압축이 성능에 미치는 영향 측정

---

## 코드 변경 내역

### 1. `buildSrc/src/main/kotlin/Libs.kt`

```kotlin
// 추가
val okhttp3_coroutines = okhttp("okhttp-coroutines")  // OkHttp 5.x 공식 코루틴 지원
```

### 2. `io/http/build.gradle.kts`

```kotlin
// testImplementation 블록에 추가
compileOnly(Libs.okhttp3_coroutines)
```

### 3. `HttpClientBenchmark.kt` — 기본 처리량 벤치마크

**추가된 벤치마크 메서드**:
- `okhttp3Coroutines()` — `Call.executeAsync()` (okhttp-coroutines 공식 라이브러리)
- `javaHttpCoroutines()` — `jdkClient.sendAwait(...)` + `Dispatchers.IO`
- `javaHttpH2Coroutines()` — HTTP/2 + `sendAwait()`

**변경 사항**:
- `hc5AsyncCoroutines()`: 수동 `FutureCallback + suspendCancellableCoroutine` → `hc5Async.executeSuspending(request).code` 단순화
- 모든 코루틴 벤치마크: `Dispatchers.Default` / `Dispatchers.Unconfined` → `Dispatchers.IO`
- `ahcOptimizedCoroutinesUnconfined` 제거 (Unconfined는 프로덕션 불안전)

### 4. `HttpClientLatencyBenchmark.kt` — 고지연 환경 벤치마크 (전면 재작성)

**변경 전**: WireMock Docker 4대를 라운드로빈으로 분산  
**변경 후**: `BluetapeHttpServer` (Docker) 단일 서버 사용

| 항목 | 이전 | 이후 |
|------|------|------|
| 서버 | WireMock × 4 (인위적 분산) | BluetapeHttpServer × 1 (실제 HTTP 서버) |
| 지연 | 50ms (WireMock 고정) | `/httpbin/delay/0.05` (서버 측 50ms) |
| `@Threads` | 100 | 100 |
| `connPerHost` | n × 서버수 | 200 (단일 서버) |
| 라운드로빈 로직 | `ThreadLocalRandom` + `pickUrl()` | 제거 |

**이론값**: 100 threads × (1000ms / 50ms) = 2,000 ops/s (동기 상한)

### 5. `HttpClientCompressionCacheBenchmark.kt` — 캐시 + gzip 벤치마크 (전면 재작성)

**변경 전**: MockWebServer (동일 JVM) — 네트워크 지연 없음으로 캐시 효과 측정 불가  
**변경 후**: WireMockServer (Docker, 10ms 고정 지연)

**핵심 수정 — RFC 7234 필수 헤더 추가**:

```kotlin
val now = ZonedDateTime.now(ZoneId.of("UTC"))
val httpDate = now.format(DateTimeFormatter.RFC_1123_DATE_TIME)
val lastModified = now.minusDays(1).format(DateTimeFormatter.RFC_1123_DATE_TIME)

WireMock.ok()
    .withHeader("Cache-Control", "public, max-age=3600")
    .withHeader("Date", httpDate)           // ← 이 헤더 없으면 HC5가 캐싱 거부
    .withHeader("Last-Modified", lastModified)
    .withHeader("Vary", "Accept-Encoding")  // ← OkHttp gzip 협상용
    .withHeader("Content-Encoding", "gzip")
    .withFixedDelay(DELAY_MS)
    .withBody(gzipBytes)
```

> HC5 `CachingHttpClient`는 RFC 7234를 엄격히 준수하여 `Date` 헤더 없으면 캐싱을 완전히 거부한다.
> OkHttp는 관대하여 `Date` 없이도 `Cache-Control: max-age`만으로 캐싱.

**@TearDown에 캐시 통계 출력 추가**:

```kotlin
okhttpCachedClient.cache?.let { cache ->
    println("hitRate: ${cache.hitCount() * 100.0 / cache.requestCount()}%")
}
```

### 6. `OkHttpDiskCacheVerificationTest.kt` (신규)

OkHttp `DiskLruCache`가 실제로 캐시 히트를 반환하는지 단위 테스트로 검증:
- `r.networkResponse == null` — 캐시 히트 시 네트워크 없음
- `r.cacheResponse != null` — 캐시 응답 존재
- 단일 스레드 캐시 속도 > 1,000 ops/s (no-cache 100 ops/s 대비 10배 이상)

### 7. `testing/mock-server/src/main/resources/application.yml`

```yaml
# 변경 전
server:
  tomcat:
    threads:
      max: 200  # Spring Boot 기본값

# 변경 후
server:
  tomcat:
    threads:
      max: 16000
    max-connections: 16000
    accept-count: 16000
```

**변경 이유**: `@Threads(100)` JMH 벤치마크에서 각 스레드가 50ms 요청을 보낼 때,
기본 Tomcat 스레드 200개가 서버 측 병목이 되어 클라이언트 성능 측정이 왜곡됨.
Docker 이미지 재빌드(`./gradlew :bluetape4k-mock-server:jibDockerBuild`)로 적용.

---

## 벤치마크 결과

### HttpClientCompressionCacheBenchmark

**환경**: WireMockServer Docker · 10ms 고정 지연 · gzip 1KB · `Cache-Control: public, max-age=3600` · `@Threads(8)` · warmup 2×3s · measurement 3×5s

| 클라이언트 | 캐시 | ops/s | 배율 |
|-----------|------|------:|------|
| HC5 Classic + InMemoryCache | Heap ConcurrentHashMap | **813,906** | ×1,233 |
| OkHttp3 + DiskLruCache | 파일 (OS 페이지 캐시) | **35,359** | ×53 |
| HC5 Classic (캐시 없음) | — | 682 | ×1 (기준) |
| HC5 Classic VirtualThread | — | 668 | — |
| OkHttp3 (캐시 없음) | — | 661 | — |

---

## 핵심 인사이트

### 캐시 효과

10ms 네트워크 지연 제거만으로:
- HC5 MemCache: **1,233배** 향상 (682 → 813,906 ops/s)
- OkHttp DiskCache: **53배** 향상 (661 → 35,359 ops/s)

### HC5 MemCache vs OkHttp DiskCache (23배 차이) 분석

| | HC5 InMemoryCache | OkHttp DiskLruCache |
|--|--|--|
| 저장소 | Java Heap `ConcurrentHashMap` | 파일시스템 (`DiskLruCache`) |
| 잠금 방식 | Lock-free (ConcurrentHashMap) | `synchronized(DiskLruCache)` |
| 추가 I/O | 없음 | journal append (LRU 갱신) |
| gzip 처리 | 저장 시 1회 해제 | 매 히트마다 재해제 |
| 예상 지연 | ~1–10 μs/op | ~200–230 μs/op |

**OkHttp DiskCache 35K가 의심스럽지 않은 이유**:
- 1KB 캐시 파일 = 단일 4KB OS 페이지 → 워밍업 후 OS 페이지 캐시(RAM)에 상주
- 실제 디스크 I/O 없음. 하지만 파일시스템 시스템 콜 + journal write 오버헤드 존재
- 8 threads × 4,420 ops/thread = 35,359 ops/s → 229μs/op ≈ 예상값과 일치
- 단위 테스트(`OkHttpDiskCacheVerificationTest`)로 캐시 히트 실증 확인

### 처리량 이외의 고려 사항 (이번 세션 미측정)

순수 처리량(ops/s)만 보면 HC5 Classic과 OkHttp3가 비슷하지만,
**CPU 점유율 및 리소스 효율** 관점에서는 차이가 있다:

| 방식 | 처리량 | CPU 효율 | 적합한 상황 |
|------|--------|----------|-------------|
| HC5 Classic (플랫폼 스레드) | 기준 | 낮음 — 스레드 블로킹 | 단순 동기 호출 |
| HC5 Classic + Virtual Threads | ≈ 동일 | **높음** — VT는 경량 | 고동시성 동기 스타일 |
| HC5 Async + Coroutines | ≈ 동일 ~ 높음 | **높음** — 스레드 비블로킹 | 고지연 대량 요청 |
| OkHttp3 + Coroutines | ≈ 동일 | 중간 | 범용 |

고지연(50ms+) 환경에서 100+ 동시 요청 시:
- 플랫폼 스레드: 100스레드 × 50ms 블로킹 → 5,000ms CPU 대기
- Virtual Thread / 비동기: 동일 처리량을 훨씬 적은 OS 스레드로 달성

→ **종합 권장**: 처리량 + CPU 효율을 함께 고려하면 `HC5 Classic + Virtual Thread` 또는 `HC5 Async + Coroutines` 가 장기적으로 더 나은 선택.

---

## 이번 세션 프로덕션 코드 수정

- [x] `VirtualThreadHttpClient.kt`: 미사용 `vtExecutor` dead code 제거
- [x] `OkHttp3Support.kt`: `maxIdleConnections` CPU코어수 → 50, `okhttp3DispatcherWithVirtualThread()` maxRequests/maxRequestsPerHost 파라미터 추가 (기본 200/100)
- [x] `CachingHttpClientBuilder.kt`: `memoryCachingHttpClientOf()` → `CachingHttpClients.createMemoryBound()` (synchronized LinkedHashMap) → `InMemoryHttpCacheStorage` (ConcurrentHashMap) 교체

---

## 다음 세션 TODO

### HTTP 설정 최적화 (프로덕션 코드 적용)

- [ ] **Connection Timeout / Socket Timeout 기본값 명시**
  - HC5: `RequestConfig.custom().setConnectionRequestTimeout()` / `setResponseTimeout()` 기본값 설정
  - OkHttp3: `connectTimeout`, `readTimeout`, `writeTimeout` 기본값 통일 (현재 10s/10s/30s)

- [ ] **Keep-Alive 설정 최적화**
  - HC5: `setConnectionKeepAlive(ConnectionKeepAliveStrategy)` — 서버 `Keep-Alive` 헤더 없을 때 기본 TTL 설정
  - OkHttp3: `ConnectionPool` keepAliveDuration (현재 5분, 서버별 최적값 검토)

- [ ] **Connection Eviction / Stale Check**
  - HC5: `evictExpiredConnections()`, `evictIdleConnections(timeout)` 백그라운드 스레드 활성화
  - OkHttp3: 자동 처리되나, pool 크기와 TTL 조율

- [ ] **Retry / Redirect 정책 기본값 노출**
  - HC5: `setRetryStrategy()` — 기본 재시도 횟수/조건 DSL로 노출
  - OkHttp3: `retryOnConnectionFailure` 옵션 기본값 확인

### 캐시 통합 (프로덕션 코드 적용)

- [ ] **`memoryCachingHttpClientOf(maxEntries, maxObjectSize)` 파라미터 추가**
  - `CacheConfig`로 최대 엔트리 수, 객체 크기 제한 설정 가능하도록

- [ ] **`fileCachingHttpClientOf(cacheDir, maxCacheMb)` 개선**
  - HC5 파일 캐시 + `CacheConfig` 설정 파라미터 노출

- [ ] **OkHttp3 디스크 캐시 DSL 추가**
  - `okhttp3Client(cacheDir, maxCacheMb) { ... }` 형태로 쉽게 캐시 활성화

- [ ] **캐시 적중률 모니터링 확장 포인트**
  - HC5 `HttpCacheContext`에서 캐시 히트/미스 메트릭 추출 유틸리티
  - OkHttp3 `Cache.hitCount()` / `networkCount()` 로깅 인터셉터

### 기타

- [ ] `testing/mock-server` → Spring WebFlux + Netty 마이그레이션
- [ ] CPU 점유율 / GC 압력 벤치마크 추가 (async-profiler 연동)
- [ ] HC5 단일화 검토 (캐시, VT, Async, Coroutines 모두 HC5로 커버 가능한지)

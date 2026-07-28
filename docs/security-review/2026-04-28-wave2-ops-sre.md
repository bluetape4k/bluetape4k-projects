# Wave 2 Ops/SRE Reliability 검토 — 2026-04-28

전체 5개 그룹 병렬 실행 결과. Tier 2 (Ops/SRE) 기준.

## 전체 요약

| 그룹             | CRITICAL | HIGH   | MEDIUM |
|------------------|----------|--------|--------|
| core + testing   | 0        | 1      | 6      |
| io + texts       | 1        | 4      | 9      |
| data + aws       | 0        | 0      | 4      |
| infra + utils    | 1        | 4      | 4      |
| spring-boot + vt | 1        | 5      | 5      |
| **합계**         | **3**    | **14** | **28** |

---

## CRITICAL

### C1 — Vert.x 이벤트 루프 블로킹 (io/feign)

- **파일:** `io/feign/src/main/kotlin/io/bluetape4k/feign/clients/vertx/VertxHttpClient.kt:56-58`
- **이슈:** `execute()`가 `CompletableFuture.get(timeout, unit)` 동기 대기 → Vert.x 이벤트 루프 스레드에서 호출 시 전체 루프 차단.
- **수정:** `Context.isOnEventLoopThread()` 검증 후 reject, 또는 비동기 전용 API로 재설계

### C2 — LettuceLock.lock () 타임아웃 없는 busy-spin (infra/lettuce)

- **파일:** `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceLock.kt:137-146`
- **이슈:** lock holder가 죽으면 TTL이 갱신되지 않아 무한 대기. `lockAsync()`는 `maxWaitTime=5분` 있으나 `lock()`은 없음.
- **수정:** `lock(maxWaitTime: Duration = Duration.ofMinutes(5))` 파라미터 추가

### C3 — ApiExceptionHandler 스택 트레이스 REST 응답 body 포함 (spring-boot3+4)

- **파일:**
    - `spring-boot3/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptionHandler.kt:39`
    - `spring-boot3/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiErrorResponse.kt:44`
    - `spring-boot4` 동일 미러
- **이슈:** `stackTraces: List<StackTraceElement>` 필드가 모든 에러 응답에 포함. 클래스명/패키지/버전 노출.
- **수정:** `stackTraces` 필드 제거 또는 `@Profile("dev")` 조건부 활성화. 서버 로그에만 기록.

---

## HIGH

### H1 — KLoggingChannel JVM 종료 시 로그 유실 (core)

- **파일:** `bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/coroutines/KLoggingChannel.kt:43-47, 80-88`
-

**이슈:** `SharedFlow(extraBufferCapacity=64, SUSPEND)` 버퍼가 있으나 종료 훅이 `job.cancel()` 만 호출 (flush 없음). 종료 직전 ERROR/WARN 이벤트 손실.
- **수정:** `runBlocking { withTimeout(2.seconds) { flushAndJoin(job) } }` 후 `cancelAndJoin()`

### H2 — Vert.x resilience4j 스케줄러 매 호출 생성 + 미종료 (io/vertx)

- **파일:**
    - `io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRetrySupport.kt:22, 43`
    - `io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureTimeLimiterSupport.kt:23, 42`
    - `io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecorators.kt:83, 91`
- **이슈:** `Executors.newSingleThreadScheduledExecutor()` 매 호출 생성, `shutdown()` 없음 → 스레드 + 메모리 누수.
- **수정:** 모듈 레벨 lazy 공유 스케줄러 (RetrofitCallSupport 패턴 참조)

### H3 — Suspend CSV writers 블로킹 I/O withContext 없음 (io/csv)

- **파일:**
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordWriter.kt:50-98`
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/FlowCsvWriterImpl.kt:51-72`
- **이슈:** `suspend` 함수 내에서 `withContext(Dispatchers.IO)` 없이 블로킹 `Writer.write()` 직접 호출 → Dispatchers.Default 고갈.
- **수정:** suspend 메서드 내부를 `withContext(Dispatchers.IO) { ... }` 로 감싸기

### H4 — DictionaryProvider InputStream 미닫힘 (texts/tokenizer-core)

- **파일:** `texts/tokenizer-core/src/main/kotlin/io/bluetape4k/tokenizer/utils/DictionaryProvider.kt:42-76, 161-202`
-

**이슈:** `readStreamByLine`이 `buffered().lineSequence()` 반환 시 Reader 미닫힘. short-circuit 시 InputStream 누수. 추가로 `readWordsAsSet`/`readWords`가 `suspend` 이지만 `withContext(IO)` 없음.
- **수정:** `useLines { }` consumer 패턴으로 교체

### H5 — OkHttp executeSuspending 취소 race에서 Response 미닫힘 (io/http)

- **파일:** `io/http/src/main/kotlin/io/bluetape4k/http/okhttp3/OkHttpClientExtensionsCoroutines.kt:54-58`
- **이슈:** coroutine 취소 후 `onResponse` 도착 시 `response.close()` 없음 → 커넥션 풀 누수.
- **수정:** `if (!cont.isActive) { response.close(); return }` 추가

### H6 — AbstractWebClientConfig InsecureTrustManagerFactory 기본값 (spring-boot3+4)

- **파일:**
    - `spring-boot3/core/src/main/kotlin/io/bluetape4k/spring/webflux/config/AbstractWebClientConfig.kt:66-70`
    - `spring-boot4` 동일 미러
- **이슈:** 기본 `sslContext()` = `InsecureTrustManagerFactory.INSTANCE` → 모든 인증서 수락.
- **수정:** 시스템 trust store 기본값 사용, `insecureSslContext()` opt-in 헬퍼 제공

### H7 — DataInitializer CoroutineScope @PreDestroy 없음 (spring-boot3+4)

- **파일:**
    - `spring-boot3/exposed-r2dbc-demo/src/main/kotlin/io/bluetape4k/examples/exposed/webflux/config/DataInitializer.kt:28,30,72-74`
    - `spring-boot4` 동일 미러
- **이슈:** `AutoCloseable.close()` Spring이 자동 호출 안 함 → 종료 시 coroutine scope 누수.
- **수정:** `@PreDestroy fun destroy() { scope.cancel() }` 또는 `DisposableBean` 구현

### H8 — Spring Batch fault-tolerance API 없음 (spring-boot3+4)

- **파일:**
    - `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/dsl/BatchJobExtensions.kt:26-49`
    - `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/writer/ExposedItemWriter.kt:36-46`
    - `spring-boot4` 동일 미러
- **이슈:** `faultTolerant()`, `skipLimit`, `retry`, `skipPolicy` 없음. 1건 실패 시 500개 청크 전체 롤백.
- **수정:** `faultTolerantStep()`, `skipLimit`, `retryLimit` DSL 헬퍼 추가

### H9 — Actuator HealthIndicator 없음 (spring-boot3+4)

- **파일:** 전체 `spring-boot3/4` src/main 에 `HealthIndicator` 구현 없음
- **이슈:** Lettuce near-cache, R2DBC pool, Cassandra session 상태를 Spring Actuator /health로 확인 불가.
- **수정:** 주요 의존성별 `ReactiveHealthIndicator` 빈 추가

### H10 — LettuceLock/LettuceSemaphore Thread.sleep VT 핀닝 (infra/lettuce)

- **파일:**
    - `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceLock.kt:112, 144`
    - `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/semaphore/LettuceSemaphore.kt:172`
- **이슈:** sync 재시도 루프에서 `Thread.sleep` → VT carrier thread 핀닝.
- **수정:** `LockSupport.parkNanos(...)` 또는 async 변형 사용

### H11 — LettuceClients.kt connectTimeout 하드코딩 (infra/lettuce)

- **파일:** `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt:61`
- **이슈:** `connectTimeout(Duration.ofSeconds(5))` 하드코딩 → SRE 튜닝 불가.
- **수정:** 파라미터화 또는 시스템 프로퍼티 지원

### H12 — Kafka deserialization null 반환 (poison pill 처리 불가) (infra/kafka)

- **파일:** `infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt:99-105`
- **이슈:** 모든 `Throwable` 잡아서 `null` 반환 → tombstone과 구분 불가, DLQ 라우팅 불가.
- **수정:** `SerializationException` throw 또는 typed error 반환

### H13 — LettuceSuspendedLoadedMap.close () runBlocking 데드락 위험 (infra/lettuce)

- **파일:** `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMap.kt:328`
- **이슈:** `close()` 내부에서 `runBlocking { flushBatch(batch) }` → coroutine dispatcher에서 호출 시 데드락.
- **수정:** `suspend fun closeAsync()` 노출 또는 `Dispatchers.IO.limitedParallelism(1)` 에서 실행

### H14 — WorkAdapters.asBlocking () coroutine 컨텍스트에서 데드락 (utils/workflow)

- **파일:** `utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/WorkAdapters.kt:33-35`
- **이슈:** `SuspendWork.asBlocking()` = `runBlocking { execute(ctx) }`. KDoc 경고만 있고 런타임 guard 없음.
- **수정:** `require(currentCoroutineContext()[Job] == null)` 런타임 검증 추가

---

## MEDIUM (28건 — 파일/라인 포함 주요 항목)

### core + testing (M1~M6)

- M1: `Ignite3Server.kt:162,172,191,218` — HTTP 클라이언트 per-request timeout 없음
- M2: `DeferredValue.kt:25-29` — 상위 scope 없는 eager async, caller 포기 후 계속 실행
- M3: `Ignite3Server.kt:227` — `Thread.sleep(2000)` VT 핀닝 (최대 60초)
- M4: `RedisServer.kt:202-204` — Redisson timeout/retryAttempts 하드코딩
- M5: `RedisServer.kt:258` — `Thread.sleep` VT 핀닝 (최대 4.5초)
- M6: `AwaitilityCoroutines.kt:121-131` — user ignorer가 CancellationException 삼킬 수 있음

### io + texts (M7~M15)

- M7: `JdkHttpClientSupport.kt:35-66` — connect-timeout만 설정, per-request timeout 없음
- M8: `OkHttp3Support.kt:62-64` — timeout 값 하드코딩 (10s/10s/30s)
- M9: `FeignRequestSupport.kt:22-23` — `defaultRequestOptions` 공유 가변 싱글톤
- M10: `AbstractGrpcServer.kt:89-102` — `awaitTermination` timeout 시 `shutdownNow` 없음
- M11: `PoolSupport.kt:46,86` — non-SQL Throwable을 `SQLException`으로 래핑 (원인 은닉)
- M12: CSV writer `close()` 오류 무음 삼킴
- M13: `JsonMapperSupport.kt` (jackson2+3) — `runCatching {...}.getOrNull()` 파싱 오류 무음
- M14: Tink 클래스 전체 KLogging 없음 (암호화 실패 감사 추적 불가)
- M15: Compressor 클래스 부모 로거 상속 확인 필요

### data + aws (M16~M19)

- M16: `DynamoDbCoroutineRepository.kt:127-133` — `deleteAll` 무제한 fan-out (최대 64 동시 요청)
- M17: `TrinoDatabase.kt:106-114` — raw `DriverManager.getConnection` (풀링 없음)
- M18: `TransactionExtensions.kt:17-18,48-55` — KDoc vs 실제 CancellationException 처리 불일치
- M19: `TransactionSupport.kt:45` — `R2dbcTransactionManager` 매 호출 생성

### infra + utils (M20~M23)

- M20: `CacheInvalidationStrategy.kt:64-80` — Redis 실패 로그 없음
- M21: `SearchApiCoroutines.kt:160-166` — ES PIT close 실패 무음
- M22: `WorkAdapters.kt:33-35` — KDoc 경고만 있는 `asBlocking` (런타임 guard 없음)
- M23: `LettuceSuspendedLoadedMap` 읽기 path 스탬피드 보호 미확인

### spring-boot + vt (M24~M28)

- M24: `VirtualThreads.kt:29`, `StructuredScopes.kt:176` — `by lazy {}` (SYNCHRONIZED) VT 초기화 핀닝 → `LazyThreadSafetyMode.PUBLICATION` 권장
- M25: `ExposedR2dbcConfig.kt:53-62` — R2DBC pool 값 하드코딩 (`@ConfigurationProperties` 미사용)
- M26: `AbstractWebClientConfig.kt:50` — WebClient responseTimeout 3초 (운영 환경에서 부족)
- M27: `ExposedItemWriter.kt:36-46` — 실패 로그 없음 (성공 debug만)
- M28: `Jdk21StructuredTaskScopeProvider.kt:164-167` — `@Suppress("UNCHECKED_CAST")` 취약 cast

---

## 수정 우선순위

| 순위 | 심각도   | 건수 |
|------|----------|------|
| 1    | CRITICAL | 3    |
| 2    | HIGH     | 14   |
| 3    | MEDIUM   | 28   |

---

*Wave 1 결과: docs/security-review/2026-04-28-wave1-security.md*
*Wave 3~5 결과는 동일 디렉토리에 추가 예정*
